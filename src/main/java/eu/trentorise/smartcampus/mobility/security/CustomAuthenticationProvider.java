package eu.trentorise.smartcampus.mobility.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

	@Autowired
	private AppSetup appSetup;
	
	public CustomAuthenticationProvider() {
		super();
	}

	@Override
	protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
	}
	
	@Override
	protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
		AppInfo app = appSetup.findAppById(username);
		if (app == null) {
			throw new UsernameNotFoundException(username);
		}
		if (!app.getPassword().equals(authentication.getCredentials().toString())) {
			throw new BadCredentialsException("Incorrect password");
		}
		return new AppDetails(app);
	}

}