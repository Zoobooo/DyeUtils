package dev.zoobooo.dyeutils.skin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MojangProfilesTest {
	@Test
	void readsABatchOfIds() {
		String json = """
				[{"id":"069a79f444e94726a5befca90e38aaf5","name":"Notch"},
				 {"id":"853c80ef3c3749fdaa49938b674adae6","name":"jeb_"}]
				""";

		Map<String, UUID> ids = MojangProfiles.parseIds(json);

		assertEquals(2, ids.size());
		assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), ids.get("notch"));
		assertEquals(UUID.fromString("853c80ef-3c37-49fd-aa49-938b674adae6"), ids.get("jeb_"));
	}

	@Test
	void namesThatDoNotExistAreAbsentRatherThanNull() {
		String json = """
				[{"id":"069a79f444e94726a5befca90e38aaf5","name":"Notch"}]
				""";

		Map<String, UUID> ids = MojangProfiles.parseIds(json);

		assertEquals(1, ids.size());
		assertNull(ids.get("thisuserdoesnotexist12345xyz"));
	}

	@Test
	void idsAreKeyedInLowerCaseWhateverMojangSends() {
		Map<String, UUID> ids = MojangProfiles.parseIds("""
				[{"id":"069a79f444e94726a5befca90e38aaf5","name":"NoTcH"}]
				""");

		assertTrue(ids.containsKey("notch"));
	}

	@Test
	void survivesRubbish() {
		assertTrue(MojangProfiles.parseIds(null).isEmpty());
		assertTrue(MojangProfiles.parseIds("").isEmpty());
		assertTrue(MojangProfiles.parseIds("Not found").isEmpty());
		assertTrue(MojangProfiles.parseIds("{\"not\":\"an array\"}").isEmpty());
		assertTrue(MojangProfiles.parseIds("[{\"name\":\"Notch\"}]").isEmpty(), "no id, so nothing usable");
		assertTrue(MojangProfiles.parseIds("[{\"id\":\"nonsense\",\"name\":\"Notch\"}]").isEmpty());
	}

	@Test
	void readsTheTextureProperty() {
		String json = """
				{"id":"069a79f444e94726a5befca90e38aaf5","name":"Notch","profileActions":[],
				 "properties":[{"name":"textures","value":"ewogICJ0aW1lc3RhbXAiIDog","signature":"c2ln"}]}
				""";

		Optional<MojangProfiles.Textures> textures = MojangProfiles.parseTextures(json);

		assertTrue(textures.isPresent());
		assertEquals("ewogICJ0aW1lc3RhbXAiIDog", textures.get().value());
		assertEquals("c2ln", textures.get().signature());
	}

	@Test
	void anUnsignedPropertyIsStillUsable() {
		Optional<MojangProfiles.Textures> textures = MojangProfiles.parseTextures("""
				{"id":"069a79f444e94726a5befca90e38aaf5","name":"Notch",
				 "properties":[{"name":"textures","value":"ewogICJ0aW1lc3RhbXAiIDog"}]}
				""");

		assertTrue(textures.isPresent());
		assertNull(textures.get().signature(), "the skin still loads, it is just not verifiable");
	}

	@Test
	void ignoresPropertiesThatAreNotTextures() {
		Optional<MojangProfiles.Textures> textures = MojangProfiles.parseTextures("""
				{"properties":[{"name":"somethingelse","value":"nope"}]}
				""");

		assertTrue(textures.isEmpty());
	}

	@Test
	void aProfileWithNoPropertiesIsEmptyNotAnError() {
		assertTrue(MojangProfiles.parseTextures("{\"id\":\"x\",\"name\":\"Notch\"}").isEmpty());
		assertTrue(MojangProfiles.parseTextures(null).isEmpty());
		assertTrue(MojangProfiles.parseTextures("<html>502 Bad Gateway</html>").isEmpty());
	}

	@Test
	void undashedAndDashedIdsBothParse() {
		UUID expected = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

		assertEquals(expected, MojangProfiles.parseUuid("069a79f444e94726a5befca90e38aaf5"));
		assertEquals(expected, MojangProfiles.parseUuid("069a79f4-44e9-4726-a5be-fca90e38aaf5"));
		assertNull(MojangProfiles.parseUuid("obviously not a uuid"));
	}

	@Test
	void emptyInputIsNotARequest() {
		assertEquals(Map.of(), MojangProfiles.ids(List.of()));
	}
}
