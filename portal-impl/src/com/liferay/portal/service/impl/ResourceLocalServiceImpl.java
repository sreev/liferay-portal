/**
 * Copyright (c) 2000-2012 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.service.impl;

import com.liferay.portal.ResourceActionsException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.model.AuditedModel;
import com.liferay.portal.model.ClassedModel;
import com.liferay.portal.model.GroupedModel;
import com.liferay.portal.model.PermissionedModel;
import com.liferay.portal.model.Resource;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.impl.ResourceImpl;
import com.liferay.portal.security.permission.PermissionCacheUtil;
import com.liferay.portal.security.permission.PermissionThreadLocal;
import com.liferay.portal.security.permission.ResourceActionsUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.base.ResourceLocalServiceBaseImpl;
import com.liferay.portal.util.ResourcePermissionsThreadLocal;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.time.StopWatch;

/**
 * The Resource local service is responsible for accessing, creating, modifying,
 * and deleting resources.
 *
 * <p>
 * Permissions in Liferay are defined for resource/action pairs. Some resources,
 * known as portlet resources, define actions that end-user can perform with
 * respect to a portlet window. Other resources, known as model resources,
 * define actions that the end-user can perform with respect to the
 * service/persistence layer.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @author Wilson S. Man
 * @author Raymond Augé
 * @author Julio Camarero
 * @author Connor McKay
 */
public class ResourceLocalServiceImpl extends ResourceLocalServiceBaseImpl {

	/**
	 * Adds model resources for the model. Four resources are created for the
	 * model with individual, group, group template, and company scopes,
	 * respectively.
	 *
	 * @param  auditedModel the model corresponding to the resources
	 * @param  serviceContext the resource's service context. Can set whether to
	 *         derive default permissions for the model.
	 * @throws PortalException if a PortalException occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addModelResources(
			AuditedModel auditedModel, ServiceContext serviceContext)
		throws PortalException, SystemException {

		ClassedModel classedModel = (ClassedModel)auditedModel;

		if (serviceContext.isAddGroupPermissions() ||
			serviceContext.isAddGuestPermissions()) {

			addResources(
				auditedModel.getCompanyId(), getGroupId(auditedModel),
				auditedModel.getUserId(), classedModel.getModelClassName(),
				String.valueOf(classedModel.getPrimaryKeyObj()), false,
				serviceContext.isAddGroupPermissions(),
				serviceContext.isAddGuestPermissions(),
				getPermissionedModel(auditedModel));
		}
		else {
			if (serviceContext.isDeriveDefaultPermissions()) {
				serviceContext.deriveDefaultPermissions(
					getGroupId(auditedModel), classedModel.getModelClassName());
			}

			addModelResources(
				auditedModel.getCompanyId(), getGroupId(auditedModel),
				auditedModel.getUserId(), classedModel.getModelClassName(),
				String.valueOf(classedModel.getPrimaryKeyObj()),
				serviceContext.getGroupPermissions(),
				serviceContext.getGuestPermissions(),
				getPermissionedModel(auditedModel));
		}
	}

	/**
	 * Adds model resources for the model using a numerical representation of
	 * the model's primary key. Four resources are created for the model with
	 * individual, group, group template, and company scopes, respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  userId the primary key of user adding the resources
	 * @param  name the name of the type of resource. By convention, it is
	 *         recommended to use the class name of the model.
	 * @param  primKey a numerical representation of the primary key of the
	 *         current instance of the model, optionally <code>0</code> if there
	 *         is no current instance of the model.
	 * @param  groupPermissions the group permissions to be added
	 * @param  guestPermissions the guest permissions to be added
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addModelResources(
			long companyId, long groupId, long userId, String name,
			long primKey, String[] groupPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		addModelResources(
			companyId, groupId, userId, name, String.valueOf(primKey),
			groupPermissions, guestPermissions, null);
	}

	/**
	 * Adds model resources for the model using the model's primary key. Four
	 * resources are created for the model with individual, group, group
	 * template, and company scopes, respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  userId the primary key of user adding the resources
	 * @param  name the name of the type of resource. By convention, it is
	 *         recommended to use the class name of the model.
	 * @param  primKey the primary key of the current instance of the model,
	 *         optionally empty if there is no current instance of the model.
	 * @param  groupPermissions the group permissions to be added
	 * @param  guestPermissions the guest permissions to be added
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addModelResources(
			long companyId, long groupId, long userId, String name,
			String primKey, String[] groupPermissions,
			String[] guestPermissions)
		throws PortalException, SystemException {

		addModelResources(
			companyId, groupId, userId, name, primKey, groupPermissions,
			guestPermissions, null);
	}

	/**
	 * Adds resources using a numerical representation of the entry's primary
	 * key. Four resources are created for the entry with individual, group,
	 * group template, and company scopes, respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  userId the primary key of the user adding the resources
	 * @param  name the name of the type of resource. By convention, it is
	 *         recommended to use the class name that represents this type of
	 *         entry.
	 * @param  primKey the primary key of the entry, optionally <code>0</code>
	 *         if there is no current instance of the entry.
	 * @param  portletActions whether to associate portlet actions with the
	 *         resource.
	 * @param  addGroupPermissions whether to add group permissions
	 * @param  addGuestPermissions whether to add guest permissions
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addResources(
			long companyId, long groupId, long userId, String name,
			long primKey, boolean portletActions, boolean addGroupPermissions,
			boolean addGuestPermissions)
		throws PortalException, SystemException {

		addResources(
			companyId, groupId, userId, name, String.valueOf(primKey),
			portletActions, addGroupPermissions, addGuestPermissions, null);
	}

	/**
	 * Adds resources using the entry's primary key. Four resources are created
	 * for the entry with individual, group, group template, and company scopes,
	 * respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  userId the primary key of the user adding the resource
	 * @param  name the name of the type of resource. By convention, it is
	 *         recommended to use the class name that represents this type of
	 *         entry.
	 * @param  primKey the primary key of the entry, optionally empty if there
	 *         is no current instance of the entry.
	 * @param  portletActions whether to associate portlet actions with the
	 *         resource
	 * @param  addGroupPermissions whether to add group permissions
	 * @param  addGuestPermissions whether to add guest permissions
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addResources(
			long companyId, long groupId, long userId, String name,
			String primKey, boolean portletActions, boolean addGroupPermissions,
			boolean addGuestPermissions)
		throws PortalException, SystemException {

		addResources(
			companyId, groupId, userId, name, primKey, portletActions,
			addGroupPermissions, addGuestPermissions, null);
	}

	/**
	 * Adds resources without specifying a user or the entry's primary key. Four
	 * resources are created for the entry with individual, group, group
	 * template, and company scopes, respectively. Use this method if the user
	 * is unknown or irrelevant and there is no current instance of the entry.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  name the name of the type of resource. By convention, it is
	 *         recommended to use the class name that represents this type of
	 *         entry.
	 * @param  portletActions whether to associate portlet actions with the
	 *         resource
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void addResources(
			long companyId, long groupId, String name, boolean portletActions)
		throws PortalException, SystemException {

		addResources(
			companyId, groupId, 0, name, null, portletActions, false, false);
	}

	/**
	 * Deletes a model resource using a model. Note that deleting a individually
	 * scoped resource also deletes corresponding group-scoped, group
	 * template-scoped, and company-scoped resources, if they exist.
	 *
	 * @param  auditedModel the model corresponding to the resource
	 * @param  scope the scope of the resource. For more information see {@link
	 *         com.liferay.portal.model.ResourceConstants}
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void deleteResource(AuditedModel auditedModel, int scope)
		throws PortalException, SystemException {

		ClassedModel classedModel = (ClassedModel)auditedModel;

		deleteResource(
			auditedModel.getCompanyId(), classedModel.getModelClassName(),
			scope, String.valueOf(classedModel.getPrimaryKeyObj()),
			getPermissionedModel(auditedModel));
	}

	/**
	 * Deletes a resource using a numerical representation of the primary key of
	 * the entry corresponding to the resource. Note that deleting a
	 * individually scoped resource also deletes corresponding group-scoped,
	 * group template-scoped, and company-scoped resources, if they exist.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  name the name of the type of resource
	 * @param  scope the scope of the resource. For more information see {@link
	 *         com.liferay.portal.model.ResourceConstants}
	 * @param  primKey the primary key of the entry corresponding to resource
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void deleteResource(
			long companyId, String name, int scope, long primKey)
		throws PortalException, SystemException {

		deleteResource(companyId, name, scope, String.valueOf(primKey), null);
	}

	/**
	 * Deletes a resource using the primary key of the entry corresponding to
	 * the resource. Note that deleting a individually scoped resource also
	 * deletes corresponding group-scoped, group template-scoped, and
	 * company-scoped resources, if they exist.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  name the name of the type of resource
	 * @param  scope the scope of the resource. For more information see {@link
	 *         com.liferay.portal.model.ResourceConstants}
	 * @param  primKey the primary key of the entry corresponding to the
	 *         resource
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void deleteResource(
			long companyId, String name, int scope, String primKey)
		throws PortalException, SystemException {

		deleteResource(companyId, name, scope, primKey, null);
	}

	/**
	 * Returns the specified resource.
	 *
	 * @param companyId the primary key of the portal instance
	 * @param name the name of the type of resource
	 * @param scope the scope of the resource. For more information see {@link
	 *        com.liferay.portal.model.ResourceConstants}
	 * @param primKey the primary key of the resource
	 */
	public Resource getResource(
		long companyId, String name, int scope, String primKey) {

		Resource resource = new ResourceImpl();

		resource.setCompanyId(companyId);
		resource.setName(name);
		resource.setScope(scope);
		resource.setPrimKey(primKey);

		return resource;
	}

	/**
	 * Returns <code>true</code> if the roles have permission to perform the
	 * action.
	 *
	 * @param  userId the primary key of the user performing the permission
	 *         check
	 * @param  resourceId the primary key of the resource for which permissions
	 *         are to be checked
	 * @param  resources the resources for which permissions are to be checked
	 * @param  actionId the primary key of the action to be performed on the
	 *         resources
	 * @param  roleIds the primary keys of the roles whose permissions are to be
	 *         checked
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public boolean hasUserPermissions(
			long userId, long resourceId, List<Resource> resources,
			String actionId, long[] roleIds)
		throws PortalException, SystemException {

		StopWatch stopWatch = null;

		if (_log.isDebugEnabled()) {
			stopWatch = new StopWatch();

			stopWatch.start();
		}

		int block = 1;

		boolean hasUserPermissions =
			resourcePermissionLocalService.hasResourcePermission(
				resources, roleIds, actionId);

		logHasUserPermissions(userId, resourceId, actionId, stopWatch, block++);

		return hasUserPermissions;
	}

	/**
	 * Updates the model resources for the model. Four resources are updated for
	 * the entry with individual, group, group template, and company scopes,
	 * respectively.
	 *
	 * @param  auditedModel the model corresponding to the resources
	 * @param  serviceContext the resource's service context. Can set default
	 *         group permissions and default guest permissions.
	 * @throws PortalException if a PortalException occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void updateModelResources(
			AuditedModel auditedModel, ServiceContext serviceContext)
		throws PortalException, SystemException {

		ClassedModel classedModel = (ClassedModel)auditedModel;

		updateResources(
			auditedModel.getCompanyId(), getGroupId(auditedModel),
			classedModel.getModelClassName(),
			String.valueOf(classedModel.getPrimaryKeyObj()),
			serviceContext.getGroupPermissions(),
			serviceContext.getGuestPermissions(),
			getPermissionedModel(auditedModel));
	}

	/**
	 * Updates resources using a numerical representation of the entry's primary
	 * key. Four resources are updated for the entry with individual, group,
	 * group template, and company scopes, respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  name the name of the type of resource
	 * @param  primKey the primary key of the entry corresponding to the
	 *         resources
	 * @param  groupPermissions the group permissions to be added
	 * @param  guestPermissions the guest permissions to be added
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void updateResources(
			long companyId, long groupId, String name, long primKey,
			String[] groupPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		updateResources(
			companyId, groupId, name, String.valueOf(primKey), groupPermissions,
			guestPermissions, null);
	}

	/**
	 * Updates resources using the entry's primary key. Four resources are
	 * updated for the entry with individual, group, group template, and company
	 * scopes, respectively.
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  groupId the primary key of the group
	 * @param  name the name of the type of resource
	 * @param  primKey the primary key of the entry corresponding to the
	 *         resources
	 * @param  groupPermissions the group permissions to be added
	 * @param  guestPermissions the guest permissions to be added
	 * @throws PortalException if a portal exception occurred
	 * @throws SystemException if a system exception occurred
	 */
	public void updateResources(
			long companyId, long groupId, String name, String primKey,
			String[] groupPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		updateResources(
			companyId, groupId, name, primKey, groupPermissions,
			guestPermissions, null);
	}

	/**
	 * Updates resources, replacing the primary key of the entry corresponding
	 * to the resources with a new primary key
	 *
	 * @param  companyId the primary key of the portal instance
	 * @param  name the name of the type of resource
	 * @param  scope the scope of the resource. For more information see {@link
	 *         com.liferay.portal.model.ResourceConstants}
	 * @param  primKey the old primary key of the entry corresponding to the
	 *         resources
	 * @param  newPrimKey the new primary key of the entry corresponding to the
	 *         resources
	 * @throws SystemException if a system exception occurred
	 */
	public void updateResources(
			long companyId, String name, int scope, String primKey,
			String newPrimKey)
		throws SystemException {

		if (resourceBlockLocalService.isSupported(name)) {

			// Assuming that this method is used when the primary key of an
			// existing record is changed, then nothing needs to happen here, as
			// it should still have its resource block ID.

		}
		else {
			updateResourcePermissions(
				companyId, name, scope, primKey, newPrimKey);
		}
	}

	protected void addGroupPermissions(
			long companyId, long groupId, long userId, String name,
			Resource resource, boolean portletActions,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		List<String> actions = null;

		if (portletActions) {
			actions = ResourceActionsUtil.getPortletResourceGroupDefaultActions(
				name);
		}
		else {
			actions = ResourceActionsUtil.getModelResourceGroupDefaultActions(
				name);
		}

		String[] actionIds = actions.toArray(new String[actions.size()]);

		if (resourceBlockLocalService.isSupported(name)) {
			addGroupPermissionsBlocks(
				groupId, resource, actions, permissionedModel);
		}
		else {
			addGroupPermissions(groupId, resource, actionIds);
		}
	}

	protected void addGroupPermissions(
			long groupId, Resource resource, String[] actionIds)
		throws PortalException, SystemException {

		Role role = roleLocalService.getDefaultGroupRole(groupId);

		resourcePermissionLocalService.setResourcePermissions(
			resource.getCompanyId(), resource.getName(), resource.getScope(),
			resource.getPrimKey(), role.getRoleId(), actionIds);
	}

	protected void addGroupPermissionsBlocks(
			long groupId, Resource resource, List<String> actionIds,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (permissionedModel == null) {
			throw new IllegalArgumentException("Permissioned model is null");
		}

		// Scope is assumed to always be individual

		Role role = roleLocalService.getDefaultGroupRole(groupId);

		resourceBlockLocalService.setIndividualScopePermissions(
			resource.getCompanyId(), groupId, resource.getName(),
			permissionedModel, role.getRoleId(), actionIds);
	}

	protected void addGuestPermissions(
			long companyId, long groupId, long userId, String name,
			Resource resource, boolean portletActions,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		List<String> actions = null;

		if (portletActions) {
			actions = ResourceActionsUtil.getPortletResourceGuestDefaultActions(
				name);
		}
		else {
			actions = ResourceActionsUtil.getModelResourceGuestDefaultActions(
				name);
		}

		String[] actionIds = actions.toArray(new String[actions.size()]);

		if (resourceBlockLocalService.isSupported(name)) {
			addGuestPermissionsBlocks(
				companyId, groupId, resource, actions, permissionedModel);
		}
		else {
			addGuestPermissions(companyId, resource, actionIds);
		}
	}

	protected void addGuestPermissions(
			long companyId, Resource resource, String[] actionIds)
		throws PortalException, SystemException {

		Role guestRole = roleLocalService.getRole(
			companyId, RoleConstants.GUEST);

		resourcePermissionLocalService.setResourcePermissions(
			resource.getCompanyId(), resource.getName(), resource.getScope(),
			resource.getPrimKey(), guestRole.getRoleId(), actionIds);
	}

	protected void addGuestPermissionsBlocks(
			long companyId, long groupId, Resource resource,
			List<String> actionIds, PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (permissionedModel == null) {
			throw new IllegalArgumentException("Permissioned model is null");
		}

		// Scope is assumed to always be individual

		Role guestRole = roleLocalService.getRole(
			companyId, RoleConstants.GUEST);

		resourceBlockLocalService.setIndividualScopePermissions(
			resource.getCompanyId(), groupId, resource.getName(),
			permissionedModel, guestRole.getRoleId(), actionIds);
	}

	protected void addModelResources(
			long companyId, long groupId, long userId, Resource resource,
			String[] groupPermissions, String[] guestPermissions,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		// Owner permissions

		Role ownerRole = roleLocalService.getRole(
			companyId, RoleConstants.OWNER);

		List<String> ownerActionIds =
			ResourceActionsUtil.getModelResourceActions(resource.getName());

		ownerActionIds = ListUtil.copy(ownerActionIds);

		filterOwnerActions(resource.getName(), ownerActionIds);

		String[] ownerPermissions = ownerActionIds.toArray(
			new String[ownerActionIds.size()]);

		// Group permissions

		Role defaultGroupRole = null;

		if (groupId > 0) {
			defaultGroupRole = roleLocalService.getDefaultGroupRole(groupId);

			if (groupPermissions == null) {
				groupPermissions = new String[0];
			}
		}

		// Guest permissions

		Role guestRole = roleLocalService.getRole(
			companyId, RoleConstants.GUEST);

		if (guestPermissions == null) {
			guestPermissions = new String[0];
		}

		if (resourceBlockLocalService.isSupported(resource.getName())) {

			if (permissionedModel == null) {
				throw new IllegalArgumentException(
					"Permissioned model is null");
			}

			// Scope is assumed to always be individual

			resourceBlockLocalService.setIndividualScopePermissions(
				resource.getCompanyId(), groupId, resource.getName(),
				permissionedModel, ownerRole.getRoleId(), ownerActionIds);

			if (groupId > 0) {
				resourceBlockLocalService.setIndividualScopePermissions(
					resource.getCompanyId(), groupId, resource.getName(),
					permissionedModel, defaultGroupRole.getRoleId(),
					Arrays.asList(groupPermissions));
			}

			resourceBlockLocalService.setIndividualScopePermissions(
				resource.getCompanyId(), groupId, resource.getName(),
				permissionedModel, guestRole.getRoleId(),
				Arrays.asList(guestPermissions));
		}
		else {
			resourcePermissionLocalService.setOwnerResourcePermissions(
				resource.getCompanyId(), resource.getName(),
				resource.getScope(), resource.getPrimKey(),
				ownerRole.getRoleId(), userId, ownerPermissions);

			if (groupId > 0) {
				resourcePermissionLocalService.setResourcePermissions(
					resource.getCompanyId(), resource.getName(),
					resource.getScope(), resource.getPrimKey(),
					defaultGroupRole.getRoleId(), groupPermissions);
			}

			resourcePermissionLocalService.setResourcePermissions(
				resource.getCompanyId(), resource.getName(),
				resource.getScope(), resource.getPrimKey(),
				guestRole.getRoleId(), guestPermissions);
		}
	}

	protected void addModelResources(
			long companyId, long groupId, long userId, String name,
			String primKey, String[] groupPermissions,
			String[] guestPermissions, PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (!PermissionThreadLocal.isAddResource()) {
			return;
		}

		validate(name, false);

		if (primKey == null) {
			return;
		}

		// Individual

		Resource resource = getResource(
			companyId, name, ResourceConstants.SCOPE_INDIVIDUAL, primKey);

		// Permissions

		boolean flushEnabled = PermissionThreadLocal.isFlushEnabled();

		PermissionThreadLocal.setIndexEnabled(false);

		try {
			addModelResources(
				companyId, groupId, userId, resource, groupPermissions,
				guestPermissions, permissionedModel);
		}
		finally {
			PermissionThreadLocal.setIndexEnabled(flushEnabled);

			PermissionCacheUtil.clearCache();

			SearchEngineUtil.updatePermissionFields(name, primKey);
		}
	}

	protected void addResources(
			long companyId, long groupId, long userId, Resource resource,
			boolean portletActions, PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		List<String> actionIds = null;

		if (portletActions) {
			actionIds = ResourceActionsUtil.getPortletResourceActions(
				resource.getName());
		}
		else {
			actionIds = ResourceActionsUtil.getModelResourceActions(
				resource.getName());

			actionIds = ListUtil.copy(actionIds);

			filterOwnerActions(resource.getName(), actionIds);
		}

		Role role = roleLocalService.getRole(companyId, RoleConstants.OWNER);

		if (resourceBlockLocalService.isSupported(resource.getName())) {
			if (permissionedModel == null) {
				throw new IllegalArgumentException(
					"Permissioned model is null");
			}

			// Scope is assumed to always be individual

			resourceBlockLocalService.setIndividualScopePermissions(
				resource.getCompanyId(), groupId, resource.getName(),
				permissionedModel, role.getRoleId(), actionIds);
		}
		else {
			resourcePermissionLocalService.setOwnerResourcePermissions(
				resource.getCompanyId(), resource.getName(),
				resource.getScope(), resource.getPrimKey(), role.getRoleId(),
				userId, actionIds.toArray(new String[actionIds.size()]));
		}
	}

	protected void addResources(
			long companyId, long groupId, long userId, String name,
			String primKey, boolean portletActions, boolean addGroupPermissions,
			boolean addGuestPermissions, PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (!PermissionThreadLocal.isAddResource()) {
			return;
		}

		validate(name, portletActions);

		if (primKey == null) {
			return;
		}

		// Individual

		Resource resource = getResource(
			companyId, name, ResourceConstants.SCOPE_INDIVIDUAL, primKey);

		// Permissions

		boolean flushEnabled = PermissionThreadLocal.isFlushEnabled();

		PermissionThreadLocal.setIndexEnabled(false);

		List<ResourcePermission> resourcePermissions =
			resourcePermissionPersistence.findByC_N_S_P(
				companyId, name, ResourceConstants.SCOPE_INDIVIDUAL, primKey);

		ResourcePermissionsThreadLocal.setResourcePermissions(
			resourcePermissions);

		try {
			addResources(
				companyId, groupId, userId, resource, portletActions,
				permissionedModel);

			// Group permissions

			if ((groupId > 0) && addGroupPermissions) {
				addGroupPermissions(
					companyId, groupId, userId, name, resource, portletActions,
					permissionedModel);
			}

			// Guest permissions

			if (addGuestPermissions) {

				// Don't add guest permissions when you've already added group
				// permissions and the given group is the guest group.

				addGuestPermissions(
					companyId, groupId, userId, name, resource, portletActions,
					permissionedModel);
			}
		}
		finally {
			ResourcePermissionsThreadLocal.setResourcePermissions(null);

			PermissionThreadLocal.setIndexEnabled(flushEnabled);

			PermissionCacheUtil.clearCache();

			SearchEngineUtil.updatePermissionFields(name, primKey);
		}
	}

	protected void deleteResource(
			long companyId, String name, int scope, String primKey,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (resourceBlockLocalService.isSupported(name)) {
			if (permissionedModel == null) {
				throw new IllegalArgumentException(
					"Permissioned model is null");
			}

			resourceBlockLocalService.releasePermissionedModelResourceBlock(
				permissionedModel);

			return;
		}

		resourcePermissionLocalService.deleteResourcePermissions(
			companyId, name, scope, primKey);
	}

	protected void filterOwnerActions(String name, List<String> actionIds) {
		List<String> defaultOwnerActions =
			ResourceActionsUtil.getModelResourceOwnerDefaultActions(name);

		if (defaultOwnerActions.isEmpty()) {
			return;
		}

		Iterator<String> itr = actionIds.iterator();

		while (itr.hasNext()) {
			String actionId = itr.next();

			if (!defaultOwnerActions.contains(actionId)) {
				itr.remove();
			}
		}
	}

	protected long getGroupId(AuditedModel auditedModel) {
		long groupId = 0;

		if (auditedModel instanceof GroupedModel) {
			GroupedModel groupedModel = (GroupedModel)auditedModel;

			groupId = groupedModel.getGroupId();
		}

		return groupId;
	}

	protected PermissionedModel getPermissionedModel(
		AuditedModel auditedModel) {

		PermissionedModel permissionedModel = null;

		if (auditedModel instanceof PermissionedModel) {
			permissionedModel = (PermissionedModel)auditedModel;
		}

		return permissionedModel;
	}

	protected void logHasUserPermissions(
		long userId, long resourceId, String actionId, StopWatch stopWatch,
		int block) {

		if (!_log.isDebugEnabled()) {
			return;
		}

		_log.debug(
			"Checking user permissions block " + block + " for " + userId +
				" " + resourceId + " " + actionId + " takes " +
					stopWatch.getTime() + " ms");
	}

	protected void updateResourceBlocks(
			long companyId, long groupId, Resource resource,
			String[] groupPermissions, String[] guestPermissions,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		if (permissionedModel == null) {
			throw new IllegalArgumentException("Permissioned model is null");
		}

		// Scope is assumed to always be individual

		Role role = roleLocalService.getDefaultGroupRole(groupId);

		resourceBlockLocalService.setIndividualScopePermissions(
			companyId, groupId, resource.getName(), permissionedModel,
			role.getRoleId(), Arrays.asList(groupPermissions));

		role = roleLocalService.getRole(companyId, RoleConstants.GUEST);

		resourceBlockLocalService.setIndividualScopePermissions(
			companyId, groupId, resource.getName(), permissionedModel,
			role.getRoleId(), Arrays.asList(guestPermissions));
	}

	protected void updateResourcePermissions(
			long companyId, long groupId, Resource resource,
			String[] groupPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		Role role = roleLocalService.getDefaultGroupRole(groupId);

		resourcePermissionLocalService.setResourcePermissions(
			resource.getCompanyId(), resource.getName(), resource.getScope(),
			resource.getPrimKey(), role.getRoleId(), groupPermissions);

		role = roleLocalService.getRole(companyId, RoleConstants.GUEST);

		resourcePermissionLocalService.setResourcePermissions(
			resource.getCompanyId(), resource.getName(), resource.getScope(),
			resource.getPrimKey(), role.getRoleId(), guestPermissions);
	}

	protected void updateResourcePermissions(
			long companyId, String name, int scope, String primKey,
			String newPrimKey)
		throws SystemException {

		List<ResourcePermission> resourcePermissions =
			resourcePermissionLocalService.getResourcePermissions(
				companyId, name, scope, primKey);

		for (ResourcePermission resourcePermission : resourcePermissions) {
			resourcePermission.setPrimKey(newPrimKey);

			resourcePermissionPersistence.update(resourcePermission);
		}
	}

	protected void updateResources(
			long companyId, long groupId, String name, String primKey,
			String[] groupPermissions, String[] guestPermissions,
			PermissionedModel permissionedModel)
		throws PortalException, SystemException {

		Resource resource = getResource(
			companyId, name, ResourceConstants.SCOPE_INDIVIDUAL, primKey);

		if (groupPermissions == null) {
			groupPermissions = new String[0];
		}

		if (guestPermissions == null) {
			guestPermissions = new String[0];
		}

		if (resourceBlockLocalService.isSupported(name)) {
			updateResourceBlocks(
				companyId, groupId, resource, groupPermissions,
				guestPermissions, permissionedModel);
		}
		else {
			updateResourcePermissions(
				companyId, groupId, resource, groupPermissions,
				guestPermissions);
		}
	}

	protected void validate(String name, boolean portletActions)
		throws PortalException {

		List<String> actions = null;

		if (portletActions) {
			actions = ResourceActionsUtil.getPortletResourceActions(name);
		}
		else {
			actions = ResourceActionsUtil.getModelResourceActions(name);
		}

		if (actions.size() == 0) {
			throw new ResourceActionsException(
				"There are no actions associated with the resource " + name);
		}
	}

	private static Log _log = LogFactoryUtil.getLog(
		ResourceLocalServiceImpl.class);

}