# Kitei Lessio Security Manager

A SecurityManager to spotlight and minimize IO access while allowing
fine-grained control access to IO resources.

It disables by default access to most outside resources. If a test needs
access to file and/or network resources, it can annotate the test class
to declare these resources.

## Supported annotations

### @AllowDNSResolution

Allows forward and reverse DNS lookups using the JDK methods.

This annotation takes no parameters.

Lookups of "localhost", 127.0.0.1 (IPv4) and ::1 (IPv6) are always
allowed.

Note: Custom DNS resolvers should implement the security checking as
described in SecurityManager#checkConnect: "A port number of -1
indicates that the calling method is attempting to determine the IP
address of the specified host name."

### @AllowExternalProcess

Allows spawning of external processes. 

This annotation takes no parameters.

The AllowExternalProcess annotation also allows access to file
descriptor I/O (otherwise only allowed with
`@AllowLocalFileAccess(paths={"%FD%"})`).

Note: Calling File#canExecute() also checks this permission.

### @AllowLocalFileAccess

Controls access to the local file system. Access is controlled through
full pathes or placeholders.

This annotation takes a 'paths' parameter:

```
@AllowLocalFileAccess(paths={"/a/path", "%TMP_DIR%"})
```

- %TMP_DIR% allows access to all files in the temporary folder (as set by java.io.tmpdir).
- %TMP_DIR%/[part] allows access to files that are in the temporary folder and start with [part].
- %FD% allows access to any file descriptor.
- a literal * allows access to any file.
- *[part]* allows IO to any path which contains [part].
- *[part] allows IO to any path which ends with [part].
- [part]* allows IO to any path which starts with [part].
-  All other values allow IO to any path which exactly matches the value.

The paths are checked by string compare, it is possible that a path
that may be allowed but contains parent directory references ('..') or
multiple slashes ('//') is not detected correctly. Also this is very
likely to not work on Windows.

Note: Access to all files under the jdk home directory (java.home), the
class path and to /dev/random and /dev/urandom is always allowed.


### @AllowNetworkAccess

### @AllowNetworkListen

### @AllowNetworkMulticast

### @AllowAll


## Notes and Acknowledgements

Kitei Lessio is a fork of Ness Computing testing-lessio (https://github.com/NessComputing/testing-lessio) which in turn is a fork of the LessIOSecurityManager from kawala (https://github.com/wealthfront/kawala). Unlike both of those, it contains basic support for TestNG. 

See NOTICE.txt for copyright information.
