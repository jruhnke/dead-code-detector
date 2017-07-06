/*
 * Copyright 2008 by Emeric Vernat
 *
 *     This file is part of Dead Code Detector.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dcd;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Classe utilis�e pour rassembler le contexte d'analyse et pour afficher les r�sultats.
 * @author evernat
 */
class Result {
	// ici, on utilise des Set, ordonn�s ou non, car les contenus sont uniques et surtout
	// car l'utilisation de (Linked)HashSet est ici l�g�rement plus rapide que ArrayList
	// (Implementation Patterns p108 � 111)
	private final Map<String, Set<String>> methodsByClassMap = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> fieldsByClassMap = new HashMap<String, Set<String>>();
	private final Map<String, String> superClassByClassMap = new HashMap<String, String>();
	private final Map<String, Set<String>> subClassListByClassMap = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> javaMethodListByClassMap = new HashMap<String, Set<String>>();
	private final Set<String> javaLangObjectMethods = getJavaMethods(Type
			.getInternalName(Object.class));
	private final Report report;

	/**
	 * Impl�mentation de l'interface MethodVisitor d'ASM utilis�e lors de l'analyse
	 * (parcours des appels de m�thodes et d'attributs).
	 */
	private class CallersMethodVisitor extends MethodVisitor {
		CallersMethodVisitor() {
			super(Opcodes.ASM4);
		}

		/** {@inheritDoc} */
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			// les classes java et javax ne sont pas audit�es
			if (!DcdHelper.isJavaClass(owner)) {
				//log("\t" + owner + " " + name + " " + desc);
				methodCalled(owner, name, desc);
			}
		}

		private boolean isFieldRead(int opcode) {
			return opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC;
		}

		/** {@inheritDoc} */
		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			// les classes java et javax ne sont pas audit�es
			if (isFieldRead(opcode) && !DcdHelper.isJavaClass(owner)) {
				//log("\t" + owner + " " + name + " " + desc);
				fieldCalled(owner, name, desc);
			}
		}

		/** {@inheritDoc} */
		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}

		/** {@inheritDoc} */
		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return null;
		}

		/** {@inheritDoc} */
		@Override
		public void visitAttribute(Attribute arg0) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitCode() {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitEnd() {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitIincInsn(int arg0, int arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitInsn(int arg0) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitIntInsn(int arg0, int arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitJumpInsn(int arg0, Label arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitLabel(Label arg0) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitLdcInsn(Object arg0) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitLineNumber(int arg0, Label arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitLocalVariable(String arg0, String arg1, String arg2, Label arg3,
				Label arg4, int arg5) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitMaxs(int arg0, int arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitMultiANewArrayInsn(String arg0, int arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1, boolean arg2) {
			return null;
		}

		/** {@inheritDoc} */
		@Override
		public void visitTableSwitchInsn(int arg0, int arg1, Label arg2, Label... arg3) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitTryCatchBlock(Label arg0, Label arg1, Label arg2, String arg3) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitTypeInsn(int arg0, String arg1) {
			// rien
		}

		/** {@inheritDoc} */
		@Override
		public void visitVarInsn(int arg0, int arg1) {
			// rien
		}
	}

	Result(Report report) {
		super();
		this.report = report;
	}

	MethodVisitor createCallersMethodVisitor() {
		return new CallersMethodVisitor();
	}

	void clear() {
		this.methodsByClassMap.clear();
		this.fieldsByClassMap.clear();
		this.superClassByClassMap.clear();
		this.subClassListByClassMap.clear();
		this.javaMethodListByClassMap.clear();
	}

	void registerSuperClass(String asmSuperClassName, String asmClassName) {
		this.superClassByClassMap.put(asmClassName, asmSuperClassName);
	}

	void registerSubClass(String asmSuperClassName, String asmClassName) {
		Set<String> subClassList = this.subClassListByClassMap.get(asmSuperClassName);
		if (subClassList == null) {
			subClassList = new HashSet<String>(1);
			this.subClassListByClassMap.put(asmSuperClassName, subClassList);
		}
		subClassList.add(asmClassName);
	}

	private Set<String> getAllSubClasses(String className) {
		Set<String> subClassList = subClassListByClassMap.get(className);
		if (subClassList == null) {
			return Collections.emptySet();
		}
		final Set<String> allSubClasses = new HashSet<String>(subClassList);
		while (!subClassList.isEmpty()) {
			final Set<String> subClasses = new HashSet<String>();
			for (final String subClass : subClassList) {
				final Set<String> subSubClassList = subClassListByClassMap.get(subClass);
				if (subSubClassList != null) {
					subClasses.addAll(subSubClassList);
				}
			}
			allSubClasses.addAll(subClasses);
			subClassList = subClasses;
		}
		return allSubClasses;
	}

	void registerMethods(String asmClassName, Set<String> methods) {
		if (!methods.isEmpty()) {
			methodsByClassMap.put(asmClassName, methods);
		}
	}

	void registerFields(String asmClassName, Set<String> fields) {
		if (!fields.isEmpty()) {
			fieldsByClassMap.put(asmClassName, fields);
		}
	}

	void excludeJavaMethods(ClassReader classReader, Set<String> methods) {
		// premier filtre : si mode public, on exclue
		// les m�thodes qui impl�mentent ou surchargent une m�thode Java,
		// par exemple actionPerformed, equals, hashCode
		// (le filtre final sera fait dans filterJavaMethods)
		if (DcdHelper.isJavaClass(classReader.getSuperName())) {
			excludeJavaMethods(methods, classReader.getSuperName());
		} else {
			methods.removeAll(javaLangObjectMethods);
		}
		for (final String interfaceName : classReader.getInterfaces()) {
			excludeJavaMethods(methods, interfaceName);
		}
	}

	private void excludeJavaMethods(Set<String> methods, String asmClassName) {
		if (!methods.isEmpty() && DcdHelper.isJavaClass(asmClassName)) {
			methods.removeAll(getJavaMethods(asmClassName));
		}
	}

	void filterJavaMethods() {
		for (final Map.Entry<String, Set<String>> entry : subClassListByClassMap.entrySet()) {
			final String className = entry.getKey();
			if (!DcdHelper.isJavaClass(className)) {
				continue;
			}
			final Set<String> javaMethods = getJavaMethods(className);
			for (final String subClass : getAllSubClasses(className)) {
				final Set<String> methods = methodsByClassMap.get(subClass);
				if (methods != null) {
					methods.removeAll(javaMethods);
					if (methods.isEmpty()) {
						methodsByClassMap.remove(subClass);
					}
				}
			}
		}
	}

	private Set<String> getJavaMethods(String asmClassName) {
		Set<String> methods = javaMethodListByClassMap.get(asmClassName);
		if (methods == null) {
			methods = DcdHelper.getJavaMethods(asmClassName);
			javaMethodListByClassMap.put(asmClassName, methods);
		}
		return methods;
	}

	void methodCalled(String className, String name, String desc) {
		methodCalled(className, name, desc, methodsByClassMap);
	}

	void fieldCalled(String className, String name, String desc) {
		methodCalled(className, name, desc, fieldsByClassMap);
	}

	private void methodCalled(String className, String name, String desc,
			Map<String, Set<String>> targetMap) {
		// Gestion des appels directs sur un objet ou une classe.
		methodCalledInternal(className, name, desc, targetMap);

		// Gestion des appels dynamiques sur un objet (la classe est d�duite � l'ex�cution
		// en fonction de l'h�ritage selon le principe du polymorphisme).
		// NB: Le polymorphisme sur les interfaces ne rentre en compte que si
		// on a fait l'indexation en mode public car sinon les m�thodes des interfaces �tant
		// publiques, on ne s'int�resserait qu'aux m�thodes packages ou private.

		// super-classe et super-super-classes (il arrive parfois qu'une classe fille
		// appelle une m�thode package non statique de sa classe m�re, ou bien que la m�thode de la classe m�re
		// soit appel�e par l'interm�diaire d'un objet de la classe fille par exemple)
		String superClass = superClassByClassMap.get(className);
		while (superClass != null && !DcdHelper.isJavaClass(superClass)) {
			methodCalledInternal(superClass, name, desc, targetMap);
			superClass = superClassByClassMap.get(superClass);
		}
		// sous-classes et sous-sous-classes
		// (il arrive qu'une m�thode d�finie et appel�e dans une classe m�re
		// soit surcharg�e dans une classe fille)
		for (final String subClass : getAllSubClasses(className)) {
			methodCalledInternal(subClass, name, desc, targetMap);

			// il arrive qu'une m�thode d�finie dans une super-classe
			// soit appel�e par une interface de la sous-classe
			// (par ex, GenericDAOAbstract.findAll appel�e sur l'interface MonDAO
			// qui est impl�ment�e par MonDAOImpl h�ritant de GenericDAOAbstract
			// m�me si toutes ces couches sont probablement peu utiles,
			// et cela compile au d�part car MonDAO h�rite de l'interface GenericDAO)
			String superClass2 = superClassByClassMap.get(subClass);
			while (superClass2 != null && !superClass2.equals(className)
					&& !DcdHelper.isJavaClass(superClass2)) {
				methodCalledInternal(superClass2, name, desc, targetMap);
				superClass2 = superClassByClassMap.get(superClass2);
			}
		}
	}

	private static void methodCalledInternal(String className, String name, String desc,
			Map<String, Set<String>> targetMap) {
		final Set<String> methods = targetMap.get(className);
		if (methods != null) {
			methods.remove(DcdHelper.getMethodKey(name, desc));
			if (methods.isEmpty()) {
				targetMap.remove(className);
			}
		}
	}

	int reportDeadCode(boolean publicDeadCode) throws XMLStreamException {
		int suspects = 0;
		// TreeMap pour ordre d'affichage alphab�tique par classe
		for (final Map.Entry<String, Set<String>> entry : new TreeMap<String, Set<String>>(
				methodsByClassMap).entrySet()) {
			final String asmClassName = entry.getKey();
			final Set<String> methods = entry.getValue();
			final Set<String> fields = fieldsByClassMap.remove(asmClassName);
			final Set<String> descs = new LinkedHashSet<String>(methods.size()
					+ (fields != null ? fields.size() : 0));
			if (fields != null) {
				for (final String field : fields) {
					descs.add(DcdHelper.getFieldDescription(field));
				}
				suspects += fields.size();
			}
			for (final String method : methods) {
				descs.add(DcdHelper.getMethodDescription(method));
			}
			report.reportDeadCodeSuspects(publicDeadCode, asmClassName, descs);
			suspects += methods.size();
		}
		for (final Map.Entry<String, Set<String>> entry : new TreeMap<String, Set<String>>(
				fieldsByClassMap).entrySet()) {
			final String asmClassName = entry.getKey();
			final Set<String> fields = entry.getValue();
			final Set<String> descs = new LinkedHashSet<String>(fields.size());
			for (final String field : fields) {
				descs.add(DcdHelper.getFieldDescription(field));
			}
			report.reportDeadCodeSuspects(publicDeadCode, asmClassName, descs);
			suspects += fields.size();
		}
		return suspects;
	}
}
