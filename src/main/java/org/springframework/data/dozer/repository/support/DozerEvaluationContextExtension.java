package org.springframework.data.dozer.repository.support;

import org.springframework.data.dozer.repository.query.EscapeCharacter;
import org.springframework.data.spel.spi.EvaluationContextExtension;

import lombok.RequiredArgsConstructor;

public class DozerEvaluationContextExtension implements EvaluationContextExtension {

	private final DozerRootObject root;

	/**
	 * Creates a new {@link DozerEvaluationContextExtension} for the given escape character.
	 *
	 * @param escapeCharacter the character to be used to escape parameters for LIKE expression.
	 */
	public DozerEvaluationContextExtension(char escapeCharacter) {
		this.root = DozerRootObject.of(EscapeCharacter.of(escapeCharacter));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.spel.spi.EvaluationContextExtension#getExtensionId()
	 */
	@Override
	public String getExtensionId() {
		return "dozer";
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.spel.spi.EvaluationContextExtension#getRootObject()
	 */
	@Override
	public Object getRootObject() {
		return root;
	}

	@RequiredArgsConstructor(staticName = "of")
	public static class DozerRootObject {

		private final EscapeCharacter character;

		/**
		 * Escapes the given source {@link String} for LIKE expressions.
		 *
		 * @param source can be {@literal null}.
		 * @return
		 * @see EscapeCharacter#escape(String)
		 */
		public String escape(String source) {
			return character.escape(source);
		}

		/**
		 * Returns the escape character being used to escape special characters for LIKE expressions.
		 *
		 * @return
		 */
		public char escapeCharacter() {
			return character.getEscapeCharacter();
		}
	}

}
