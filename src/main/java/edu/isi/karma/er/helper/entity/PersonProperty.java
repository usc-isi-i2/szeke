package edu.isi.karma.er.helper.entity;

import java.util.ArrayList;
import java.util.List;

public class PersonProperty {

	private String predicate;
	
	private List<String> value = null;
	
	public PersonProperty() {
		this.value = new ArrayList<String>();
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public List<String> getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value.add(value);
	}
	
	
}
