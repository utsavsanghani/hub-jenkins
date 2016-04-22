package com.blackducksoftware.integration.hub.jenkins;

import com.blackducksoftware.integration.hub.ValidationMessageEnum;
import com.blackducksoftware.integration.hub.exception.ValidationException;
import com.blackducksoftware.integration.hub.job.HubScanJobConfigValidator;

import hudson.util.FormValidation;

public class HubJenkinsScanJobConfigValidator extends HubScanJobConfigValidator<FormValidation> {
	@Override
	public FormValidation handleValidationException(final ValidationException e) {
		if (ValidationMessageEnum.WARN == e.getValidationMessage()) {
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
