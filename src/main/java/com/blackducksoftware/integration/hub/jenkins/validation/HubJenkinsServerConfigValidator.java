package com.blackducksoftware.integration.hub.jenkins.validation;

import com.blackducksoftware.integration.hub.ValidationExceptionEnum;
import com.blackducksoftware.integration.hub.exception.ValidationException;
import com.blackducksoftware.integration.hub.validate.HubServerConfigValidator;

import hudson.util.FormValidation;

public class HubJenkinsServerConfigValidator extends HubServerConfigValidator<FormValidation> {
	@Override
	public FormValidation handleValidationException(final ValidationException e) {
		if (ValidationExceptionEnum.WARN == e.getValidationExceptionEnum()) {
			return FormValidation.warning(e.getMessage());
		} else {
			return FormValidation.error(e, e.getMessage());
		}
	}

	@Override
	public FormValidation handleSuccess() {
		return FormValidation.ok();
	}

}
