# Changes

## Version 2.1 - 2014-03-20

Add a default exception for `InetAddress.getLocalHost()`.
Some libraries (e.g. Logback) will look up the hostname during initialization, which
trips a DNS permission violation.  Now this is allowed by default.

## Version 2.0 - 2014-01-27

* Added `@AllowTmpDirAccess` and `@AllowFileDescriptorIO` annotations.
* Deprecated `%TMP_DIR%` and `%FD%` pseudo paths for `@AllowLocalFileAccess`.
* `@AllowLocalFileAccess` now uses the JDK 7 Path API with glob matching.

## Version 1.0

* Initial release.

