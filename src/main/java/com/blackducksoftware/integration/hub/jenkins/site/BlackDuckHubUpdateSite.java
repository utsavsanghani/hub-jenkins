package com.blackducksoftware.integration.hub.jenkins.site;

import static hudson.util.TimeUnit2.DAYS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.security.DigestOutputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.servlet.ServletContext;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.jvnet.hudson.crypto.CertificateUtil;
import org.jvnet.hudson.crypto.SignatureOutputStream;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.StaplerRequest;

import com.trilead.ssh2.crypto.Base64;

import hudson.model.DownloadService;
import hudson.model.UpdateSite;
import hudson.util.FormValidation;
import hudson.util.TextFile;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * Majority of the code was copied from http://github.com/jenkinsci/cloudbees-plugin-gateway
 *
 */
public class BlackDuckHubUpdateSite extends UpdateSite {
	/**
	 * Our logger
	 */
	private static final Logger LOGGER = Logger.getLogger(BlackDuckHubUpdateSite.class.getName());

	private static final long DAY = DAYS.toMillis(1);

	/**
	 * The last timestamp of the data. Mirrors {@link UpdateSite#dataTimestamp}.
	 */
	private transient long dataTimestamp = -1;

	private transient volatile long lastAttempt = -1;

	/**
	 * Constructor.
	 *
	 * @param id
	 *            the ID of the update site.
	 * @param url
	 *            the url of the update site.
	 */
	public BlackDuckHubUpdateSite(final String id, final String url) {
		super(id, url);
	}

	/**
	 * Called when object has been deserialized from a stream.
	 *
	 * @return {@code this}, or a replacement for {@code this}.
	 * @throws java.io.ObjectStreamException
	 *             if the object cannot be restored.
	 * @see <a href="http://download.oracle.com/javase/1.3/docs/guide/serialization/spec/input.doc6.html">The Java
	 *      Object Serialization Specification</a>
	 */
	private Object readResolve() throws ObjectStreamException {
		setDataTimestamp(-1);
		return this;
	}

	/**
	 * returns the data timestamp.
	 *
	 */
	@Override
	public long getDataTimestamp() {
		return dataTimestamp;
	}

	/**
	 * Sets the data timestamp (and tries to propagate the change to {@link UpdateSite#dataTimestamp}
	 *
	 */
	private void setDataTimestamp(final long dataTimestamp) {
		try {
			// try reflection to be safe for the parent class changing the location
			final Field field = UpdateSite.class.getDeclaredField("dataTimestamp");
			final boolean accessible = field.isAccessible();
			try {
				field.setLong(this, dataTimestamp);
			} finally {
				if (!accessible) {
					field.setAccessible(false);
				}
			}
		} catch (final Throwable e) {
			// ignore
		}
		this.dataTimestamp = dataTimestamp;
	}

	/**
	 * When was the last time we asked a browser to check the data for us?
	 * Mirrors {@link hudson.model.UpdateSite#dataTimestamp}.
	 */
	private long getLastAttempt() {
		return lastAttempt;
	}

	private void setLastAttempt(final long lastAttempt) {
		try {
			// try reflection to be safe for the parent class changing the location
			final Field field = UpdateSite.class.getDeclaredField("lastAttempt");
			final boolean accessible = field.isAccessible();
			try {
				field.setLong(this, lastAttempt);
			} finally {
				if (!accessible) {
					field.setAccessible(false);
				}
			}
		} catch (final Throwable e) {
			// ignore
		}
		this.lastAttempt = lastAttempt;
	}

	/**
	 * This is where we store the update center data.
	 * Mirrors {@link hudson.model.UpdateSite#getDataFile()}
	 */
	private TextFile getDataFile() {
		try {
			// try reflection to be safe for the parent class changing the location
			final Method method = UpdateSite.class.getDeclaredMethod("getDataFile");
			final boolean accessible = method.isAccessible();
			try {
				method.setAccessible(true);
				return (TextFile) method.invoke(this);
			} finally {
				if (!accessible) {
					method.setAccessible(false);
				}
			}
		} catch (final Throwable e) {
			// ignore
		}
		return new TextFile(new File(Jenkins.getInstance().getRootDir(), "updates/" + getId() + ".json"));
	}

	@Override
	@Restricted(NoExternalUse.class)
	public @Nonnull FormValidation updateDirectlyNow(final boolean signatureCheck) throws IOException {
		return updateData(
				DownloadService.loadJSON(new URL(getUrl() + "?id=" + URLEncoder.encode(getId(), "UTF-8") + "&version="
						+ URLEncoder.encode(Jenkins.VERSION, "UTF-8"))), signatureCheck);
	}

	/**
	 * This is the endpoint that receives the update center data file from the browser.
	 * Mirrors {@link UpdateSite#doPostBack(org.kohsuke.stapler.StaplerRequest)} as there is no other way to override
	 * the verification of the signature.
	 */
	@Override
	public FormValidation doPostBack(final StaplerRequest req) throws IOException, GeneralSecurityException {
		return updateData(IOUtils.toString(req.getInputStream(), "UTF-8"), true);
	}

	private FormValidation updateData(final String json, boolean signatureCheck) throws IOException {
		setDataTimestamp(System.currentTimeMillis());
		final JSONObject o = JSONObject.fromObject(json);

		final int v = o.getInt("updateCenterVersion");
		if (v != 1) {
			throw new IllegalArgumentException("Unrecognized update center version: " + v);
		}

		try {
			final Field signatureCheckField = UpdateSite.class.getField("signatureCheck");
			signatureCheck = signatureCheckField.getBoolean(null);
		} catch (final Throwable t) {
			// ignore
		}

		if (signatureCheck) {
			final FormValidation e = verifySignature(o);
			if (e.kind != FormValidation.Kind.OK) {
				LOGGER.severe(e.renderHtml());
				return e;
			}
		}

		LOGGER.info("Obtained the latest update center data file for Black Duck UpdateSource " + getId());
		getDataFile().write(json);
		return FormValidation.ok();
	}

	@Override
	public FormValidation doVerifySignature() throws IOException {
		final JSONObject jsonObject = getJSONObject();

		LOGGER.info("Verifiy the signature of : " + getId());
		if (getId().equals(jsonObject.optString("id"))) {
			return verifySignature(jsonObject);
		} else {
			return super.doVerifySignature();
		}
	}

	/**
	 * Verifies the signature in the update center data file.
	 */
	private FormValidation verifySignature(final JSONObject o) throws IOException {
		try {
			FormValidation warning = null;

			final JSONObject signature = o.getJSONObject("signature");
			if (signature.isNullObject()) {
				return FormValidation.error("No signature block found in update center '" + getId() + "'");
			}
			o.remove("signature");

			final List<X509Certificate> certs = new ArrayList<X509Certificate>();
			{// load and verify certificates
				final CertificateFactory cf = CertificateFactory.getInstance("X509");
				for (final Object cert : signature.getJSONArray("certificates")) {
					final X509Certificate c = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(
							Base64.decode(cert.toString().toCharArray())));
					try {
						c.checkValidity();
					} catch (final CertificateExpiredException e) { // even if the certificate isn't valid yet,
						// we'll proceed it anyway
						warning = FormValidation.warning(e,
								String.format("Certificate %s has expired in update center '%s'", cert.toString(),
										getId()));
					} catch (final CertificateNotYetValidException e) {
						warning = FormValidation.warning(e,
								String.format("Certificate %s is not yet valid in update center '%s'", cert.toString(),
										getId()));
					}
					certs.add(c);
				}

				// all default root CAs in JVM are trusted, plus certs bundled in Jenkins
				final Set<TrustAnchor> anchors = new HashSet<TrustAnchor>(); // CertificateUtil.getDefaultRootCAs();
				final ServletContext context = Jenkins.getInstance().servletContext;
				anchors.add(new TrustAnchor(loadLicenseCaCertificate(), null));
				for (final String cert : (Set<String>) context.getResourcePaths("/WEB-INF/update-center-rootCAs")) {
					if (cert.endsWith(".txt")) {
						continue; // skip text files that are meant to be documentation
					}
					final InputStream stream = context.getResourceAsStream(cert);
					if (stream != null) {
						try {
							anchors.add(new TrustAnchor((X509Certificate) cf.generateCertificate(stream), null));
						} finally {
							IOUtils.closeQuietly(stream);
						}
					}
				}
				CertificateUtil.validatePath(certs, anchors);
			}

			// this is for computing a digest to check sanity
			final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
			final DigestOutputStream dos = new DigestOutputStream(new NullOutputStream(), sha1);

			// this is for computing a signature
			final Signature sig = Signature.getInstance("SHA1withRSA");
			sig.initVerify(certs.get(0));
			final SignatureOutputStream sos = new SignatureOutputStream(sig);

			// until JENKINS-11110 fix, UC used to serve invalid digest (and therefore unverifiable signature)
			// that only covers the earlier portion of the file. This was caused by the lack of close() call
			// in the canonical writing, which apparently leave some bytes somewhere that's not flushed to
			// the digest output stream. This affects Jenkins [1.424,1,431].
			// Jenkins 1.432 shipped with the "fix" (1eb0c64abb3794edce29cbb1de50c93fa03a8229) that made it
			// compute the correct digest, but it breaks all the existing UC json metadata out there. We then
			// quickly discovered ourselves in the catch-22 situation. If we generate UC with the correct signature,
			// it'll cut off [1.424,1.431] from the UC. But if we don't, we'll cut off [1.432,*).
			//
			// In 1.433, we revisited 1eb0c64abb3794edce29cbb1de50c93fa03a8229 so that the original "digest"/"signature"
			// pair continues to be generated in a buggy form, while "correct_digest"/"correct_signature" are generated
			// correctly.
			//
			// Jenkins should ignore "digest"/"signature" pair. Accepting it creates a vulnerability that allows
			// the attacker to inject a fragment at the end of the json.
			o.writeCanonical(new OutputStreamWriter(new TeeOutputStream(dos, sos), "UTF-8")).close();

			// did the digest match? this is not a part of the signature validation, but if we have a bug in the c14n
			// (which is more likely than someone tampering with update center), we can tell
			final String computedDigest = new String(Base64.encode(sha1.digest()));
			final String providedDigest = signature.optString("correct_digest");
			if (providedDigest == null) {
				return FormValidation.error("No correct_digest parameter in update center '" + getId()
				+ "'. This metadata appears to be old.");
			}
			if (!computedDigest.equalsIgnoreCase(providedDigest)) {
				return FormValidation
						.error("Digest mismatch: " + computedDigest + " vs " + providedDigest + " in update center '"
								+ getId() + "'");
			}

			final String providedSignature = signature.getString("correct_signature");
			if (!sig.verify(Base64.decode(providedSignature.toCharArray()))) {
				return FormValidation
						.error("Signature in the update center doesn't match with the certificate in update center '"
								+ getId() + "'");
			}

			if (warning != null) {
				return warning;
			}
			return FormValidation.ok();
		} catch (final GeneralSecurityException e) {
			return FormValidation.error(e, "Signature verification failed in the update center '" + getId() + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDue() {
		if (neverUpdate) {
			return false;
		}
		if (getDataTimestamp() == -1) {
			setDataTimestamp(getDataFile().file.lastModified());
		}
		final long now = System.currentTimeMillis();
		final boolean due = now - getDataTimestamp() > DAY && now - getLastAttempt() > 15000;
		if (due) {
			setLastAttempt(now);
		}
		return due;
	}

	/*
	 * JENKINS-13454 introduces accidental serialization of the UpdateSite's cached getData(), so
	 * as a work-around for Jenkins versions not including the fix in JENKINS-15889, for serialization,
	 * ensure the serialized data does not include the cache.
	 */
	private Object writeReplace() throws ObjectStreamException {
		return new BlackDuckHubUpdateSite(getId(), getUrl());
	}

	/* package */
	static X509Certificate loadLicenseCaCertificate() throws CertificateException {
		final CertificateFactory cf = CertificateFactory.getInstance("X.509");
		final InputStream stream = BlackDuckHubUpdateSite.class.getResourceAsStream("/blackduck-hub-root-cacert.pem");
		try {
			return stream != null ? (X509Certificate) cf.generateCertificate(stream) : null;
		} finally {
			IOUtils.closeQuietly(stream);
		}
	}

}
