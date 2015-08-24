package org.nishen.alma.toolkit.util;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

public class AlmaAuthHeaderFilter implements ClientRequestFilter
{
	private String key;

	public AlmaAuthHeaderFilter(String key)
	{
		this.key = key;
	}

	@Override
	public void filter(ClientRequestContext rc) throws IOException
	{
		String auth = "apikey " + key;
		rc.getHeaders().putSingle("Authorization", auth);
	}
}
