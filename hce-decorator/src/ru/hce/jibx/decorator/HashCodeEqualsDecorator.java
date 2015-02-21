package ru.hce.jibx.decorator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.jibx.binding.model.ElementBase;
import org.jibx.schema.codegen.IClassHolder;
import org.jibx.schema.codegen.extend.ClassDecorator;

public class HashCodeEqualsDecorator implements ClassDecorator {
	private final ASTParser m_parser = ASTParser.newParser(AST.JLS3);
	
	private List<String> fieldList = null;
	
	private String className;
	private String hashCodeFields;
	private String equalsFields;
	private String toStringFields;
	private Boolean toStringOnlyText;
	
	public void setClassName(String className) {
		this.className = className;
	}
	public void setHashCodeFields(String hashCodeFields) {
		this.hashCodeFields = hashCodeFields;
	}
	public void setEqualsFields(String equalsFields) {
		this.equalsFields = equalsFields;
	}
	public void setToStringFields(String toStringFields) {
		this.toStringFields = toStringFields;
	}
	public void setToStringOnlyText(Boolean toStringOnlyText) {
		this.toStringOnlyText = toStringOnlyText;
	}

	@Override
	public void start(IClassHolder holder) {
		fieldList = new ArrayList<String>();
	}
	
	@Override
	public void valueAdded(String basename, boolean collect, String type,
			FieldDeclaration field, MethodDeclaration getmeth,
			MethodDeclaration setmeth, String descript, IClassHolder holder) 
	{		
		fieldList.add(getmeth.getName().getFullyQualifiedName() + "()");
	}
	
	@Override
	public void finish(ElementBase base, IClassHolder holder) {
		if((className != null && holder.getFullName().equals(className)) || (className == null)) {
			createHashCode(holder);
			createEquals(holder);
			createToString(holder);
		}
	}
	
	private void createHashCode(IClassHolder holder) {
		for(MethodDeclaration method : holder.getMethods()) {
			if(method.getName().getFullyQualifiedName().equalsIgnoreCase("hashCode")) {
				return;
			}
		}
		String[] fields = null;
		if(hashCodeFields == null || hashCodeFields.trim().length() == 0) {
			fields = new String[fieldList.size()];
			for(int i = 0; i < fields.length; i++) {
				fields[i] = fieldList.get(i);
			}
		} else {
			fields = hashCodeFields.trim().replaceAll(" ", "").split(",");
		}
		holder.addImport("org.apache.commons.lang3.builder.HashCodeBuilder");
		String text = "class example { public int hashCode() { return new HashCodeBuilder(1, 31) ";
		for(String field : fields) {
			text += ".append(this." + field + ")";
		}
		text += ".toHashCode(); }}";
		addFirstMethod(text, holder);
	}
	
	private void createEquals(IClassHolder holder) {
		for(MethodDeclaration method : holder.getMethods()) {
			if(method.getName().getFullyQualifiedName().equalsIgnoreCase("equals")) {
				return;
			}
		}
		String[] fields = null;
		if(equalsFields == null || equalsFields.trim().length() == 0) {
			fields = new String[fieldList.size()];
			for(int i = 0; i < fields.length; i++) {
				fields[i] = fieldList.get(i);
			}
		} else {
			fields = equalsFields.trim().replaceAll(" ", "").split(",");
		}
		holder.addImport("org.apache.commons.lang3.builder.EqualsBuilder");
		String text = "class example { public boolean equals(Object obj) { if (this == obj) return true; if (getClass() != obj.getClass()) return false; " + holder.getName() + " other = (" + holder.getName() + ") obj; ";
		text += "return new EqualsBuilder()";
		for(String field : fields) {
			text += ".append(this." + field + ", other." + field + ")";
		}
		text += ".isEquals(); }}";
		addFirstMethod(text, holder);
	}
	
	private void createToString(IClassHolder holder) {
		for(MethodDeclaration method : holder.getMethods()) {
			if(method.getName().getFullyQualifiedName().equalsIgnoreCase("toString")) {
				return;
			}
		}
		String[] fields = null;
		if(toStringFields == null || toStringFields.trim().length() == 0) {
			fields = new String[fieldList.size()];
			for(int i = 0; i < fields.length; i++) {
				fields[i] = fieldList.get(i);
			}
		} else {
			fields = toStringFields.trim().replaceAll(" ", "").split(",");
		}
		String text = null;
		if(toStringOnlyText != null) {
			String preparedField = Character.toUpperCase(fields[0].charAt(0)) + fields[0].substring(1);
			text = "class example { public String toString() { return get" + preparedField + "(); }}";
		} else {
			holder.addImport("org.apache.commons.lang3.builder.ToStringBuilder");
			text = "class example { public String toString() { return new ToStringBuilder(this) ";
			for(String field : fields) {
				text += ".append(\"" + field + "\", this." + field + ")";
			}
			text += ".toString(); }}";
		}
		addFirstMethod(text, holder);
	}
	
	private void addFirstMethod(String text, IClassHolder holder) {
		m_parser.setSource(text.toCharArray());
        CompilationUnit unit = (CompilationUnit)m_parser.createAST(null);
        TypeDeclaration type = (TypeDeclaration)unit.types().get(0);
        MethodDeclaration method = (MethodDeclaration)type.bodyDeclarations().get(0);
        holder.addMethod(method);
	}
}
