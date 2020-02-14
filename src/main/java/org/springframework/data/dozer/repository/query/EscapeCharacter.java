package org.springframework.data.dozer.repository.query;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.lang.Nullable;

import lombok.Value;

@Value(staticConstructor = "of")
public class EscapeCharacter {

	public static final EscapeCharacter DEFAULT = EscapeCharacter.of('\\');
	private static final List<String> TO_REPLACE = Arrays.asList("_", "%");

	char escapeCharacter;

	/**
	 * Escapes all special like characters ({@code _}, {@code %}) using the configured escape character.
	 *
	 * @param value may be {@literal null}.
	 * @return
	 */
	@Nullable
	public String escape(@Nullable String value) {

		return value == null //
				? null //
				: Stream.concat(Stream.of(String.valueOf(escapeCharacter)), TO_REPLACE.stream()) //
						.reduce(value, (it, character) -> it.replace(character, this.escapeCharacter + character));
	}
}