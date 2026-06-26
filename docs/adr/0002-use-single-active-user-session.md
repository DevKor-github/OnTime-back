# Use a Single Active User Session

OnTime allows at most one active authenticated session per user. A new successful login becomes the active session and invalidates earlier access and refresh credentials, matching OWASP's simultaneous-logon guidance and Spring Security's common "expire the older session" concurrency strategy while fitting the app's stateless JWT architecture.
