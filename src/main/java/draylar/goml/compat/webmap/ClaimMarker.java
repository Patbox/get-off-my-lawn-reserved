package draylar.goml.compat.webmap;

import draylar.goml.api.Claim;
import draylar.goml.compat.webmap.player.PlayerRecord;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * Contains all details required to construct a claim marker on a web map.
 */
public final class ClaimMarker {

	// TODO: Translate to other languages

	private static final String DETAILS_CONTAINER = """
			<style>
				.section.claim-type { text-align:center;margin:12px 0 16px }
				.claim-type .label { font-size:120%%;text-decoration:none }
				.claim-type .player-icon { margin:8px 0 }
				.label { font-weight:bold;margin:0 0 8px;text-align:center;text-decoration:none;text-underline-offset:1px }
				.section { padding:0 8px;margin:0 0 16px; }
				.player-list { list-style-type:none;display:flex;flex-direction:row;flex-wrap:wrap;gap:2px;justify-content:center;padding:0;margin:0 }
				.player-record { display:flex;align-items:center;gap:4px;flex-grow:1;text-wrap:nowrap }
				.player-record img { vertical-align:middle }
			</style>
			<div class="claim-details">
				<div class="section claim-type">
					<div class="player-icon">%s</div>
					<div class="label">%s</div>
				</div>
				%s
			</div>
			""";
	private static final String LIST_SECTION_CONTAINER = """
			<div class="section">
				<div class="label">%s</div>
				<ul class="player-list">
					%s
				</ul>
			</div>
			""";
	private static final String LIST_ITEM = """
			<li style="margin:0 4px;padding:0">
				<div class="player-record">
					%s
					<span>%s</span>
				</div>
			</li>
			""";

	private final Claim claim;
	private final ServerWorld world;
	private final ClaimBox claimBox;
	private final String id;
	private final String setId;
	private final Integer color;
	private final HashMap<String,String> labels;
	private final PlayerRecord claimAnchorType;
	private final List<PlayerRecord> owners;
	private final List<PlayerRecord> trusted;
	private final List<PlayerRecord> augments;

	public ClaimMarker(Claim claim, String markerSetId, MinecraftServer server) {
		Objects.requireNonNull(claim, "Claim cannot be null");
		Objects.requireNonNull(markerSetId, "Marker set ID cannot be null");
		Objects.requireNonNull(server, "Minecraft server cannot be null");

		this.claim = claim;
		this.world = claim.getWorldInstance(server);

		if (this.world == null) {
			throw new IllegalArgumentException("Claim world instance cannot be null");
		}

		this.claimBox = claim.getClaimBox();
		if (claimBox == ClaimBox.EMPTY || claimBox.getRadius() <= 0) {
			throw new IllegalArgumentException("Claim must have a valid non-empty claim box with radius > 0");
		}

		this.id = claim.getWorld().toString() + " - " + claim.getOrigin().toShortString();
		this.setId = markerSetId;
		this.color = ClaimUtils.webMapClaimColor(claim);

		this.labels = new HashMap<String,String>(
			Map.of(
				"claim_type", getLocalizedLabel("claim_type"),
				"owners", getLocalizedLabel("owners"),
				"trusted", getLocalizedLabel("trusted"),
				"augments", getLocalizedLabel("augments")
			)
		);
		this.claimAnchorType = new PlayerRecord(
			claim.getType() instanceof PolymerHeadBlock polymerHeadBlock ? polymerHeadBlock : null,
			claim.getType().getName().getString()
		);

		this.owners = claim.getOwners().stream()
			.map(uuid -> new PlayerRecord(uuid, server, null))
			.sorted(Comparator.comparing(player -> player.getName().toLowerCase(Locale.ROOT)))
			.collect(Collectors.toList());

		this.trusted = claim.getTrusted().stream()
			.map(uuid -> new PlayerRecord(uuid, server, null))
			.sorted(Comparator.comparing(player -> player.getName().toLowerCase(Locale.ROOT)))
			.collect(Collectors.toList());

		this.augments = claim.hasAugment()
			? claim.getAugments().values().stream()
				.sorted(Comparator.comparing(augment -> augment.getAugmentName().getString().toLowerCase(Locale.ROOT)))
				.map(augment -> new PlayerRecord(
					augment instanceof PolymerHeadBlock headBlock ? headBlock : null,
					augment.getAugmentName().getString()
				))
				.collect(Collectors.toList())
			: List.of();
	}

	// Getters

	public Claim getClaim() {
		return this.claim;
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public String getId() {
		return this.id;
	}

	public String getSetId() {
		return this.setId;
	}

	public Integer getColor () {
		return this.color;
	}

	public ClaimBox getClaimBox() {
		return this.claimBox;
	}

	/**
	 * @return HTML string containing formatted claim details for web map display.
	 */
	public String renderHtml(){
		StringBuilder content = new StringBuilder();

		// Owners section
		if (!this.owners.isEmpty()) {
			StringBuilder ownersItems = new StringBuilder();
			for (PlayerRecord owner : this.owners) {
				ownersItems.append(LIST_ITEM.formatted(owner.getHeadIcon().getHtml(), owner.getName()));
			}
			String ownersSection = LIST_SECTION_CONTAINER.formatted(this.labels.get("owners"), ownersItems.toString());
			content.append(ownersSection);
		}

		// Trusted section
		if (!this.trusted.isEmpty()) {
			StringBuilder trustedItems = new StringBuilder();
			for (PlayerRecord trustedPlayer : this.trusted) {
				trustedItems.append(LIST_ITEM.formatted(trustedPlayer.getHeadIcon().getHtml(), trustedPlayer.getName()));
			}
			String trustedSection = LIST_SECTION_CONTAINER.formatted(this.labels.get("trusted"), trustedItems.toString());
			content.append(trustedSection);
		}

		// Augments section
		if (!this.augments.isEmpty()) {
			StringBuilder augmentsItems = new StringBuilder();
			for (PlayerRecord augment : this.augments) {
				augmentsItems.append(LIST_ITEM.formatted(augment.getHeadIcon().getHtml(), augment.getName()));
			}
			String augmentsSection = LIST_SECTION_CONTAINER.formatted(this.labels.get("augments"), augmentsItems.toString());
			content.append(augmentsSection);
		}

		return DETAILS_CONTAINER.formatted(
			this.claimAnchorType.getHeadIcon().getHtml("24px"),
			this.claimAnchorType.getName(),
			content.toString()
		);
	}

	private static String getLocalizedLabel(String key) {
		Text text = Text.translatable("text.goml.webmap.label." + key);
		return text == null ? "" : text.getString();
	}
}
