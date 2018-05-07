package ${packagePath}.uad.display;

import ${apiPackagePath}.model.${entity.name};
import ${apiPackagePath}.service.${entity.name}LocalService;
import ${packagePath}.uad.constants.${portletShortName}UADConstants;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.user.associated.data.display.BaseModelUADDisplay;

import java.io.Serializable;

import java.util.List;
import java.util.Locale;

import org.osgi.service.component.annotations.Reference;

/**
 * Provides the base implementation for the ${entity.name} UAD display.
 *
 * <p>
 * This implementation exists only as a container for the default methods
 * generated by ServiceBuilder. All custom methods should be put in
 * {@link ${entity.name}UADDisplay}.
 * </p>
 *
 * @author ${author}
 * @generated
 */
public abstract class Base${entity.name}UADDisplay extends BaseModelUADDisplay<${entity.name}> {

	@Override
	public ${entity.name} get(Serializable primaryKey) throws PortalException {
		return ${entity.varName}LocalService.get${entity.name}(Long.valueOf(primaryKey.toString()));
	}

	@Override
	public String getApplicationName() {
		return ${portletShortName}UADConstants.APPLICATION_NAME;
	}

	@Override
	public String[] getDisplayFieldNames() {
		return new String[]{<#list entity.UADNonanonymizableEntityColumns as uadNonanonymizableEntityColumn>"${uadNonanonymizableEntityColumn.name}"<#sep>, </#sep></#list>};
	}

	@Override
	public Class<${entity.name}> getTypeClass() {
		return ${entity.name}.class;
	}

	@Override
	protected long doCount(DynamicQuery dynamicQuery) {
		return ${entity.varName}LocalService.dynamicQueryCount(dynamicQuery);
	}

	@Override
	protected DynamicQuery doGetDynamicQuery() {
		return ${entity.varName}LocalService.dynamicQuery();
	}

	@Override
	protected List<${entity.name}> doGetRange(DynamicQuery dynamicQuery, int start, int end) {
		return ${entity.varName}LocalService.dynamicQuery(dynamicQuery, start, end);
	}

	@Override
	protected String[] doGetUserIdFieldNames() {
		return ${portletShortName}UADConstants.USER_ID_FIELD_NAMES_${entity.constantName};
	}

	@Reference
	protected ${entity.name}LocalService ${entity.varName}LocalService;

}