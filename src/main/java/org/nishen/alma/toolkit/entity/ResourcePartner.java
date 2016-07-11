package org.nishen.alma.toolkit.entity;

import java.util.Date;

public class ResourcePartner
{
	private String nuc;
	private String organisation;
	private boolean isoill;
	private boolean suspended;
	private Date suspendedFrom;
	private Date suspendedTo;

	public String getNuc()
	{
		return nuc;
	}

	public void setNuc(String nuc)
	{
		this.nuc = nuc;
	}

	public String getOrganisation()
	{
		return organisation;
	}

	public void setOrganisation(String organisation)
	{
		this.organisation = organisation;
	}

	public boolean isIsoill()
	{
		return isoill;
	}

	public void setIsoill(boolean isoill)
	{
		this.isoill = isoill;
	}

	public boolean isSuspended()
	{
		return suspended;
	}

	public void setSuspended(boolean suspended)
	{
		this.suspended = suspended;
	}

	public Date getSuspendedFrom()
	{
		return suspendedFrom;
	}

	public void setSuspendedFrom(Date suspendedFrom)
	{
		this.suspendedFrom = suspendedFrom;
	}

	public Date getSuspendedTo()
	{
		return suspendedTo;
	}

	public void setSuspendedTo(Date suspendedTo)
	{
		this.suspendedTo = suspendedTo;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (isoill ? 1231 : 1237);
		result = prime * result + ((nuc == null) ? 0 : nuc.hashCode());
		result = prime * result + ((organisation == null) ? 0 : organisation.hashCode());
		result = prime * result + (suspended ? 1231 : 1237);
		result = prime * result + ((suspendedFrom == null) ? 0 : suspendedFrom.hashCode());
		result = prime * result + ((suspendedTo == null) ? 0 : suspendedTo.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourcePartner other = (ResourcePartner) obj;
		if (isoill != other.isoill)
			return false;
		if (nuc == null)
		{
			if (other.nuc != null)
				return false;
		}
		else if (!nuc.equals(other.nuc))
			return false;
		if (organisation == null)
		{
			if (other.organisation != null)
				return false;
		}
		else if (!organisation.equals(other.organisation))
			return false;
		if (suspended != other.suspended)
			return false;
		if (suspendedFrom == null)
		{
			if (other.suspendedFrom != null)
				return false;
		}
		else if (!suspendedFrom.equals(other.suspendedFrom))
			return false;
		if (suspendedTo == null)
		{
			if (other.suspendedTo != null)
				return false;
		}
		else if (!suspendedTo.equals(other.suspendedTo))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("LaddLibrary [nuc=");
		builder.append(nuc);
		builder.append(", organisation=");
		builder.append(organisation);
		builder.append(", isoill=");
		builder.append(isoill);
		builder.append(", suspended=");
		builder.append(suspended);
		builder.append(", suspendedFrom=");
		builder.append(suspendedFrom);
		builder.append(", suspendedTo=");
		builder.append(suspendedTo);
		builder.append("]");
		return builder.toString();
	}
}
