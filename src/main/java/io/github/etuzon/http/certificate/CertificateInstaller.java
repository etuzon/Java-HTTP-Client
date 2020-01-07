package io.github.etuzon.http.certificate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import io.github.etuzon.projects.core.utils.UrlUtil;

/********************************************
 * Install machine certificate in 'jssecacerts' local file. This is used to
 * connect remote machines that have certificates on in CA.
 * 
 * @author Eyal Tuzon
 *
 */
public class CertificateInstaller {
	private static final Map<String, CertificatesManager> CERTIFICATE_URL_MAP = new HashMap<String, CertificatesManager>();

	/********************************************
	 * Install machine certificate in 'jssecacerts' local file. This is used to
	 * connect remote machines that have certificates on in CA.
	 * 
	 * @param url Remote machine URL.
	 * @throws KeyManagementException   in case failed to install certificate.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 */
	public static synchronized void installCertificate(String url) throws KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		if (CERTIFICATE_URL_MAP.get(url) == null) {
			CertificatesManager cert = new CertificatesManager(UrlUtil.getHostFromUrl(url));
			cert.installCertificate();

			synchronized (CERTIFICATE_URL_MAP) {
				CERTIFICATE_URL_MAP.put(url, cert);
			}
		}
	}

	/********************************************
	 * Get URL CertificateManager from map.
	 * 
	 * @param url URL.
	 * @return CertificateManager from map.
	 */
	public static CertificatesManager getCertificateManagerFromUrl(String url) {
		synchronized (CERTIFICATE_URL_MAP) {
			return CERTIFICATE_URL_MAP.get(url);
		}
	}
}