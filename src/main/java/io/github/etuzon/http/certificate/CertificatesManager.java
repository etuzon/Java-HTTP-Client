package io.github.etuzon.http.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/*
* Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
*   - Redistributions of source code must retain the above copyright
*     notice, this list of conditions and the following disclaimer.
*
*   - Redistributions in binary form must reproduce the above copyright
*     notice, this list of conditions and the following disclaimer in the
*     documentation and/or other materials provided with the distribution.
*
*   - Neither the name of Sun Microsystems nor the names of its
*     contributors may be used to endorse or promote products derived
*     from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
* IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
* THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
* PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
* CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
* PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
* PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
* NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

public class CertificatesManager {
	public static final String JSSECACERT_PATH = "jssecacerts";

	public static final int HTTPS_PORT = 443;
	public static final int SOCKET_TIMEOUT = 10000;
	public static final String PASSPHRASE = "changeit";

	private final String host;
	private final int port;
	private int sockeTimeout = SOCKET_TIMEOUT;;

	private SavingTrustManager tm = null;
	private static SSLContext context = null;
	private KeyStore ks = null;
	private final char[] passphrase;

	/**************************************
	 * Constructor.
	 * 
	 * Default passphrase is 'changeit'.
	 * 
	 * @param host Host.
	 * @throws KeyManagementException   in case failed to install certificate.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 */
	public CertificatesManager(String host) throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException {
		this(host, HTTPS_PORT);
	}

	/**************************************
	 * Constructor.
	 * 
	 * Default passphrase is 'changeit'.
	 * 
	 * @param host Host.
	 * @param port Port.
	 * @throws KeyManagementException   in case failed to install certificate.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 */
	public CertificatesManager(String host, int port) throws KeyManagementException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		this(host, port, PASSPHRASE);
	}

	/**************************************
	 * Constructor.
	 * 
	 * @param host          Host.
	 * @param port          Port.
	 * @param passphraseStr Certificate password.
	 * @throws KeyManagementException   in case failed to install certificate.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 */
	public CertificatesManager(String host, int port, String passphraseStr) throws KeyManagementException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		this.host = host;
		this.port = port;
		this.passphrase = passphraseStr.toCharArray();

		setJssecacertsProperty();
		initSslContext();
	}

	/**************************************
	 * Install first certificate of remote server to JSSECACERT_PATH file.
	 * 
	 * @return true in case success to install certificate, else return false.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 * @throws KeyManagementException   in case failed to install certificate.
	 */
	public boolean installCertificate() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException, KeyManagementException {
		return installCertificate(0);
	}

	/**************************************
	 * Install certificate of remote server from certificates list to
	 * JSSECACERT_PATH file.
	 * 
	 * @param chainIndex Certificate index.
	 * @return true in case success to install certificate, else return false.
	 * @throws KeyStoreException        in case failed to install certificate.
	 * @throws NoSuchAlgorithmException in case failed to install certificate.
	 * @throws CertificateException     in case failed to install certificate.
	 * @throws IOException              in case failed to install certificate.
	 * @throws KeyManagementException   in case failed to install certificate.
	 */
	public boolean installCertificate(int chainIndex) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, KeyManagementException {
		openSslSocketToGetChain();

		X509Certificate[] chain = tm.chain;

		if (chain == null) {
			return false;
		}

		X509Certificate cert = chain[chainIndex];
		String alias = host;
		ks.setCertificateEntry(alias, cert);

		return addCertificateToFile();
	}
	
	/**************************************
	 * Get certificates array.
	 * 
	 * @return certificates array.
	 */
	public X509Certificate[] getCertificatesChain() {
		return tm.chain;
	}

	/**************************************
	 * Set socket timeout in ms.
	 * 
	 * @param timeout Timeout in ms.
	 */
	public void setSocketTimeout(int timeout) {
		sockeTimeout = timeout;
	}

	/**************************************
	 * Get socket timeout in ms.
	 * 
	 * @return socket timeout in ms.
	 */
	public int getSocketTimeout() {
		return sockeTimeout;
	}

	private boolean addCertificateToFile() {
		OutputStream out = null;
		
		try {
			out = new FileOutputStream(JSSECACERT_PATH);
			ks.store(out, passphrase);
		} catch (Exception e) {
			return false;
		} finally {		
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					return false;
				}
			}
		}

		return true;
	}
	
	private void openSslSocketToGetChain() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
			IOException, KeyManagementException {
		SSLSocketFactory factory = context.getSocketFactory();

		SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
		socket.setSoTimeout(sockeTimeout);

		try {
			socket.startHandshake();
			socket.close();
		} catch (SSLException e) {
		}
	}

	private void initSslContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			KeyManagementException {
		File file = openJssecacertsFile();

		initKeyStore(file, passphrase);

		context = SSLContext.getInstance("TLS");

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ks);
		X509TrustManager defaultTrustManager = (X509TrustManager) tmf.getTrustManagers()[0];
		tm = new SavingTrustManager(defaultTrustManager);
		context.init(null, new TrustManager[] { tm }, null);
	}

	private File openJssecacertsFile() {
		File file = new File(JSSECACERT_PATH);

		if (file.isFile()) {
			return file;
		}

		char SEP = File.separatorChar;
		File dir = new File(System.getProperty("java.home") + SEP + "lib" + SEP + "security");

		if (dir.isFile() == false) {
			return new File(dir, "cacerts");
		}

		return dir;
	}

	private void initKeyStore(File file, char[] passphrase)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		InputStream in = null;

		try {
			in = new FileInputStream(file);
			ks = KeyStore.getInstance(KeyStore.getDefaultType());
			ks.load(in, passphrase);
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	/**************************************************
	 * Used for manage certificates.
	 * 
	 */
	private static class SavingTrustManager implements X509TrustManager {
		private final X509TrustManager tm;
		private X509Certificate[] chain;

		private SavingTrustManager(X509TrustManager tm) {
			this.tm = tm;
		}

		public X509Certificate[] getAcceptedIssuers() {

			/**
			 * This change has been done due to the following resolution advised for Java
			 * 1.7+ http://infposs.blogspot.kr/2013/06/installcert-and-java-7.html
			 **/

			return new X509Certificate[0];
		}

		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			throw new UnsupportedOperationException();
		}

		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			this.chain = chain;
			tm.checkServerTrusted(chain, authType);
		}
	}

	private void setJssecacertsProperty() {
		Properties systemProps = System.getProperties();
		systemProps.remove("javax.net.ssl.trustStore");
		systemProps.put("javax.net.ssl.trustStore", JSSECACERT_PATH);
		System.setProperties(systemProps);
	}

	/**************************************************
	 * Get SSLContext.
	 * 
	 * @return SSLContext.
	 */
	public static SSLContext getSslContext() {
		return context;
	}
}