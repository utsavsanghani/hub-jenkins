package com.blackducksoftware.integration.hub.jenkins.validation;

import com.blackducksoftware.integration.hub.validate.HubServerConfigValidator;
import com.blackducksoftware.integration.hub.validate.ValidationResult;
import com.blackducksoftware.integration.hub.validate.ValidationResultEnum;

import hudson.util.FormValidation;

public class HubJenkinsServerConfigValidator extends HubServerConfigValidator<FormValidation> {

	@Override
	public FormValidation processResult(final ValidationResult result) {
		if (ValidationResultEnum.WARN == result.getResultType()) {
			return FormValidation.warning(result.getMessage());
		} else if (ValidationResultEnum.ERROR == result.getResultType()) {
			return FormValidation.error(result.getMessage());
		} else {
			return FormValidation.ok();
		}
	}

}
