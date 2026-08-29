function requireIsolatedUiTestBaseUrl(env = process.env) {
  if (env.UI_TEST_ISOLATED !== '1') {
    throw new Error('Refusing to run write-enabled UI tests without UI_TEST_ISOLATED=1.');
  }

  const rawBaseUrl = String(env.UI_TEST_BASE_URL || '').trim();
  if (!rawBaseUrl) {
    throw new Error('UI_TEST_BASE_URL is required for isolated UI tests.');
  }

  let parsed;
  try {
    parsed = new URL(rawBaseUrl);
  } catch {
    throw new Error('UI_TEST_BASE_URL must be a valid http(s) URL.');
  }
  if (!['http:', 'https:'].includes(parsed.protocol)) {
    throw new Error('UI_TEST_BASE_URL must be a valid http(s) URL.');
  }
  const loopbackHosts = new Set(['localhost', '127.0.0.1', '::1', '[::1]']);
  if (!loopbackHosts.has(parsed.hostname.toLowerCase())) {
    throw new Error('UI_TEST_BASE_URL must use a loopback host for isolated UI tests.');
  }

  return rawBaseUrl.replace(/\/+$/, '');
}

module.exports = { requireIsolatedUiTestBaseUrl };
