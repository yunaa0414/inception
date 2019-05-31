package se.bth.serl.inception.coclasslinking.recommender;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CCObject implements Serializable {
	private static final long serialVersionUID = -8654253090457028818L;
	private String iri;
	private String table;
	private String code;
	private String name;
	private String definition;
	private List<String> synonyms;
	private Set<String> nouns;
	
	public CCObject() {
		synonyms = new ArrayList<>();
		nouns = new HashSet<>();
	}
	
	public String getIri() {
		return iri;
	}
	
	public void setIri(String iri) {
		this.iri = iri;
	}
	
	public String getTable() {
		return table;
	}
	
	public void setTable(String table) {
		this.table = table;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDefinition() {
		return definition;
	}
	
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	
	public List<String> getSynonyms() {
		return synonyms;
	}
	
	public void setSynonyms(List<String> synonyms) {
		this.synonyms = synonyms;
	}
	
	public void addSynonym(String synonym) {
		this.synonyms.add(synonym);
		this.nouns.add(synonym);
	}
	
	public void addNoun(String noun) {
		nouns.add(noun);
	}
	
	public Set<String> getNouns() {
		return nouns;
	}
	
	public void printNouns() {
		System.out.println(getText() + ": " + nouns.stream().collect(Collectors.joining(",")));
	}
	
	public String getText() {
		String nameString = firstUpperCaseOrNull(name);
		String definitionString = firstUpperCaseOrNull(definition);
		
		String result = "";
		if (nameString != null) {
			result += nameString + ".";
		}
		if (definitionString != null) {
			result += " " + definitionString + ".";
		}
	
		return result;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((synonyms == null) ? 0 : synonyms.hashCode());
		result = prime * result + ((table == null) ? 0 : table.hashCode());
		result = prime * result + ((iri == null) ? 0 : iri.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CCObject)) {
			return false;
		}
		CCObject other = (CCObject) obj;
		if (code == null) {
			if (other.code != null) {
				return false;
			}
		} else if (!code.equals(other.code)) {
			return false;
		}
		if (definition == null) {
			if (other.definition != null) {
				return false;
			}
		} else if (!definition.equals(other.definition)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (synonyms == null) {
			if (other.synonyms != null) {
				return false;
			}
		} else if (!synonyms.equals(other.synonyms)) {
			return false;
		}
		if (table == null) {
			if (other.table != null) {
				return false;
			}
		} else if (!table.equals(other.table)) {
			return false;
		}
		if (iri == null) {
			if (other.iri != null) {
				return false;
			}
		} else if (!iri.equals(other.iri)) {
			return false;
		}
		return true;
	}

	private String firstUpperCaseOrNull(String s) {
		if (s == null || s.length() == 0) {
			return null;
		}
		
		String result = s.substring(0, 1).toUpperCase();
		if (s.length() > 1) {
			result += s.substring(1);
		}
		
		return result;
	}
}
