/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test.schema;

import java.net.URL;
import java.util.Calendar;

import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Implementation;
import net.java.ao.ManyToMany;
import net.java.ao.Mutator;
import net.java.ao.OneToMany;
import net.java.ao.OneToOne;
import net.java.ao.Searchable;
import net.java.ao.Transient;
import net.java.ao.schema.Default;
import net.java.ao.schema.Indexed;
import net.java.ao.schema.OnUpdate;
import net.java.ao.schema.SQLType;
import net.java.ao.schema.Unique;

/**
 * @author Daniel Spiewak
 */
@Implementation(PersonImpl.class)
public interface Person extends Entity {
	
	@Searchable
	public String getFirstName();
	public void setFirstName(String firstName);
	
	@SQLType(precision=127)
	@Searchable
	public void setLastName(String lastName);
	public String getLastName();
	
	public Profession getProfession();
	public void setProfession(Profession profession);
	
	@Transient
	@Indexed
	@SQLType(precision=20)
	public int getAge();
	public void setAge(int age);

	@Unique
	@Accessor("url")
	public URL getURL();

	@Default("http://www.google.com")
	@Mutator("url")
	public void setURL(URL url);
	
	public Class<?> getFavoriteClass();
	public void setFavoriteClass(Class<?> clazz);
	
	public Company getCompany();
	public void setCompany(Company company);
	
	public byte[] getImage();
	public void setImage(byte[] image);
	
	public boolean isActive();
	public void setActive(boolean active);
	
	@OnUpdate("CURRENT_TIMESTAMP")
	public Calendar getModified();
	
	@OneToOne
	public Nose getNose();
	
	@OneToMany(where="deleted = 0")
	public Pen[] getPens();
	
	@ManyToMany(value=PersonSuit.class, where="deleted = 0")
	public PersonLegalDefence[] getPersonLegalDefences();
}
