package life.genny.utils;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.json.JsonObject;
import life.genny.channel.DistMap;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Layout;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeLink;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QBulkMessage;
import life.genny.qwanda.message.QBulkPullMessage;
import life.genny.qwanda.message.QDataAnswerMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.utils.Layout.LayoutUtils;

public class BaseEntityUtils {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private Map<String, Object> decodedMapToken;
	private String token;
	private static String realm;
	private String qwandaServiceUrl;

	private CacheUtils cacheUtil;

	public BaseEntityUtils(String qwandaServiceUrl, String token, Map<String, Object> decodedMapToken, String realm) {

		this.decodedMapToken = decodedMapToken;
		this.qwandaServiceUrl = qwandaServiceUrl;
		this.token = token;
		this.realm = realm;

		this.cacheUtil = new CacheUtils(qwandaServiceUrl, token, decodedMapToken, realm);
		this.cacheUtil.setBaseEntityUtils(this);
	}

	private static String getRealm()
	{
		return realm;
	}
	
	/* =============== refactoring =============== */

	public BaseEntity create(final String uniqueCode, final String bePrefix, final String name) {

		String uniqueId = QwandaUtils.getUniqueId(bePrefix, uniqueCode);
		if (uniqueId != null) {
			return this.create(uniqueId, name);
		}

		return null;
	}

	public BaseEntity create(String baseEntityCode, String name) {

		BaseEntity newBaseEntity = QwandaUtils.createBaseEntityByCode(baseEntityCode, name, qwandaServiceUrl,
				this.token);
		
		this.addAttributes(newBaseEntity);
		VertxUtils.writeCachedJson(newBaseEntity.getRealm(),newBaseEntity.getCode(), JsonUtils.toJson(newBaseEntity));
		return newBaseEntity;
	}

	public List<BaseEntity> getBaseEntityFromSelectionAttribute(BaseEntity be, String attributeCode) {

		List<BaseEntity> bes = new ArrayList<>();

		String attributeValue = be.getValue(attributeCode, null);
		if (attributeValue != null) {
			if (!attributeValue.isEmpty() && !attributeValue.equals(" ")) {
				/*
				 * first we try to serialise the attriute into a JsonArray in case this is a
				 * multi-selection attribute
				 */
				try {

					// JsonArray attributeValues = new JsonArray(attributeValue);

					JsonParser parser = new JsonParser();
					JsonElement tradeElement = parser.parse(attributeValue);
					JsonArray attributeValues = tradeElement.getAsJsonArray();

					/* we loop through the attribute values */
					for (int i = 0; i < attributeValues.size(); i++) {

						String beCode = attributeValues.get(i).getAsString();
						
						/* we try to fetch the base entity */
						if(beCode != null) {
							
							BaseEntity baseEntity = this.getBaseEntityByCode(beCode);
							if (baseEntity != null) {
								
								/* we add the base entity to the list */
								bes.add(baseEntity);
							}
						}
					}

					/* we return the list */
					return bes;

				} catch (Exception e) {
					/*
					 * serialisation did not work - we can assume this is a single selection
					 * attribute
					 */

					/* we fetch the BaseEntity */
					BaseEntity baseEntity = this.getBaseEntityByCode(attributeValue);

					/* we return a list containing only this base entity */
					if (baseEntity != null) {
						bes.add(baseEntity);
					}

					/* we return */
					return bes;
				}
			}
		}

		return bes;
	}

	/* ================================ */
	/* old code */

	public BaseEntity createRole(final String uniqueCode, final String name, String... capabilityCodes) {
		String code = "ROL_IS_" + uniqueCode.toUpperCase();
		log.info("Creating Role " + code + ":" + name);
		BaseEntity role = this.getBaseEntityByCode(code);
		if (role == null) {
			role = QwandaUtils.createBaseEntityByCode(code, name, qwandaServiceUrl, this.token);
			this.addAttributes(role);

			VertxUtils.writeCachedJson(role.getRealm(),role.getCode(), JsonUtils.toJson(role));
		}

		for (String capabilityCode : capabilityCodes) {
			Attribute capabilityAttribute = RulesUtils.attributeMap.get("CAP_" + capabilityCode);
			try {
				role.addAttribute(capabilityAttribute, 1.0, "TRUE");
			} catch (BadDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// Now force the role to only have these capabilitys
		try {
			String result = QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/baseentitys/force",
					JsonUtils.toJson(role), this.token);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return role;
	}

	public Object get(final String key) {
		return this.decodedMapToken.get(key);
	}

	public void set(final String key, Object value) {
		this.decodedMapToken.put(key, value);
	}

	public Attribute saveAttribute(Attribute attribute, final String token) throws IOException {

		RulesUtils.attributeMap.put(attribute.getCode(), attribute);
		try {
			String result = QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/attributes",
					JsonUtils.toJson(attribute), token);
			return attribute;
		} catch (IOException e) {
			log.error("Socket error trying to post attribute");
			throw new IOException("Cannot save attribute");
		}

	}

	public void addAttributes(BaseEntity be) {

		if (be != null) {

			if (!(be.getCode().startsWith("SBE_") || be.getCode().startsWith("RAW_"))) { // don't bother with search be
																							// or raw attributes
				for (EntityAttribute ea : be.getBaseEntityAttributes()) {
					if (ea != null) {
						Attribute attribute = RulesUtils.attributeMap.get(ea.getAttributeCode());
						if (attribute != null) {
							ea.setAttribute(attribute);
						} else {
							RulesUtils.loadAllAttributesIntoCache(this.token);
							attribute = RulesUtils.attributeMap.get(ea.getAttributeCode());
							if (attribute != null) {
								ea.setAttribute(attribute);
							} else {
								log.error("Cannot get Attribute - " + ea.getAttributeCode());

								Attribute dummy = new AttributeText(ea.getAttributeCode(), ea.getAttributeCode());
								ea.setAttribute(dummy);

							}
						}
					}
				}
			}
		}
	}

	public void saveAnswer(Answer answer) {

		try {
			this.updateCachedBaseEntity(answer);
			QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/answers", JsonUtils.toJson(answer), this.token);
			// Now update the Cache

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveAnswers(List<Answer> answers, final boolean changeEvent) {

		if (answers.size() == 1) {
			if (answers.get(0).getAttributeCode().equals("PRI_CONTAINER_SIZE_REQUESTED")) {
				log.info("PRI_CONTAINER_SIZE_REQUESTED");
			}
		}

		if (!changeEvent) {
			for (Answer answer : answers) {
				answer.setChangeEvent(false);
			}
		}
		Answer items[] = new Answer[answers.size()];
		items = answers.toArray(items);

		QDataAnswerMessage msg = new QDataAnswerMessage(items);

		this.updateCachedBaseEntity(answers);

		String jsonAnswer = JsonUtils.toJson(msg);
		jsonAnswer.replace("\\\"", "\"");

		try {
			QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/answers/bulk2", jsonAnswer, token);
		} catch (IOException e) {
			// log.error("Socket error trying to post answer");
		}
	}

	public void saveAnswers(List<Answer> answers) {
		this.saveAnswers(answers, true);
	}

	public BaseEntity getOfferBaseEntity(String groupCode, String linkCode, String linkValue, String quoterCode,
			String prefix, String attributeCode) {
		return this.getOfferBaseEntity(groupCode, linkCode, linkValue, quoterCode, true, prefix, attributeCode);
	}

	public BaseEntity getOfferBaseEntity(String groupCode, String linkCode, String linkValue, String quoterCode,
			Boolean includeHidden, String prefix, String attributeCode) {

		/*
		 * TODO : Replace with searchEntity when it will be capable of filtering based
		 * on linkWeight
		 */
		List linkList = this.getLinkList(groupCode, linkCode, linkValue, this.token);
		String quoterCodeForOffer = null;

		if (linkList != null) {

			try {
				for (Object linkObj : linkList) {

					Link link = JsonUtils.fromJson(linkObj.toString(), Link.class);
					BaseEntity offerBe = null;
					if (!includeHidden) {
						if (link.getWeight() != 0) {
							offerBe = this.getBaseEntityByCode(link.getTargetCode());
						}
					} else {
						offerBe = this.getBaseEntityByCode(link.getTargetCode());
					}

					// BaseEntity offerBe = this.getBaseEntityByCode(link.getTargetCode());

					if (offerBe != null && offerBe.getCode().startsWith(prefix)) {

						quoterCodeForOffer = offerBe.getValue(attributeCode, null);

						if (quoterCode.equals(quoterCodeForOffer)) {
							return offerBe;
						}
					}
				}
			} catch (Exception e) {

			}
		}

		return null;
	}

	public void updateBaseEntityAttribute(final String sourceCode, final String beCode, final String attributeCode,
			final String newValue) {
		List<Answer> answers = new ArrayList<Answer>();
		answers.add(new Answer(sourceCode, beCode, attributeCode, newValue));
		this.saveAnswers(answers);
	}

	public SearchEntity getSearchEntityByCode(final String code) {

		SearchEntity be = null;

		try {

			JsonObject cachedJsonObject = VertxUtils.readCachedJson(getRealm(),code);
			if (cachedJsonObject != null) {

				String seJson = JsonUtils.toJson(cachedJsonObject);
				if (seJson != null) {
					be = JsonUtils.fromJson(seJson, SearchEntity.class);
					if (be.getCode() == null) {
						return null;
					}
				}
			}

		} catch (Exception e) {
			log.error("Failed to read cache for search " + code);
		}

		return be;
	}

	public BaseEntity getBaseEntityByCode(final String code) {
		return this.getBaseEntityByCode(code, true);
	}

	public BaseEntity getBaseEntityByCode(final String code, Boolean withAttributes) {

		BaseEntity be = null;

		try {
			log.info("Fetching BaseEntityByCode, code="+code);
			be = VertxUtils.readFromDDT(getRealm(),code, withAttributes, this.token);
			if (be == null) {
				log.info("ERROR - be (" + code + ") fetched is NULL ");
			} else {
				this.addAttributes(be);
			}
		} catch (Exception e) {
			log.info("Failed to read cache for baseentity " + code);
		}

		return be;
	}

	public BaseEntity getBaseEntityByAttributeAndValue(final String attributeCode, final String value) {

		BaseEntity be = null;
		be = RulesUtils.getBaseEntityByAttributeAndValue(this.qwandaServiceUrl, this.decodedMapToken, this.token,
				attributeCode, value);
		if (be != null) {
			this.addAttributes(be);
		}
		return be;
	}

	public List<BaseEntity> getBaseEntitysByAttributeAndValue(final String attributeCode, final String value) {

		List<BaseEntity> bes = null;
		bes = RulesUtils.getBaseEntitysByAttributeAndValue(this.qwandaServiceUrl, this.decodedMapToken, this.token,
				attributeCode, value);
		return bes;
	}

	public void clearBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode, Integer pageStart,
			Integer pageSize) {

		String key = parentCode + linkCode + "-" + pageStart + "-" + pageSize;
		VertxUtils.putObject(this.realm, "LIST", key, null);
	}

	public List<BaseEntity> getBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode,
			Integer pageStart, Integer pageSize, Boolean cache) {
		cache = false;
		List<BaseEntity> bes = new ArrayList<BaseEntity>();
		String key = parentCode + linkCode + "-" + pageStart + "-" + pageSize;
		if (cache) {
			Type listType = new TypeToken<List<BaseEntity>>() {
			}.getType();
			List<String> beCodes = VertxUtils.getObject(this.realm, "LIST", key, (Class) listType);
			if (beCodes == null) {
				bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes(qwandaServiceUrl, this.decodedMapToken,
						this.token, parentCode, linkCode, pageStart, pageSize);
				beCodes = new ArrayList<String>();
				for (BaseEntity be : bes) {
					VertxUtils.putObject(this.realm, "", be.getCode(), JsonUtils.toJson(be));
					beCodes.add(be.getCode());
				}
				VertxUtils.putObject(this.realm, "LIST", key, beCodes);
			} else {
				for (String beCode : beCodes) {
					BaseEntity be = getBaseEntityByCode(beCode);
					bes.add(be);
				}
			}
		} else {

			bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes(qwandaServiceUrl, this.decodedMapToken,
					this.token, parentCode, linkCode, pageStart, pageSize);
		}

		return bes;
	}

	/* added because of the bug */
	public List<BaseEntity> getBaseEntitysByParentAndLinkCode2(final String parentCode, final String linkCode,
			Integer pageStart, Integer pageSize, Boolean cache) {

		List<BaseEntity> bes = null;

		// if (isNull("BES_" + parentCode.toUpperCase() + "_" + linkCode)) {

		bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributes2(qwandaServiceUrl, this.decodedMapToken,
				this.token, parentCode, linkCode, pageStart, pageSize);

		// } else {
		// bes = getAsBaseEntitys("BES_" + parentCode.toUpperCase() + "_" + linkCode);
		// }

		return bes;
	}

	// Adam's speedup
	public List<BaseEntity> getBaseEntitysByParentAndLinkCode3(final String parentCode, final String linkCode,
			Integer pageStart, Integer pageSize, Boolean cache) {
		cache = false;
		List<BaseEntity> bes = new ArrayList<BaseEntity>();

		BaseEntity parent = getBaseEntityByCode(parentCode);
		for (EntityEntity ee : parent.getLinks()) {
			if (ee.getLink().getAttributeCode().equalsIgnoreCase(linkCode)) {
				BaseEntity child = getBaseEntityByCode(ee.getLink().getTargetCode());

				bes.add(child);
			}
		}
		return bes;
	}

	public List<BaseEntity> getBaseEntitysByParentLinkCodeAndLinkValue(final String parentCode, final String linkCode,
			final String linkValue, Integer pageStart, Integer pageSize, Boolean cache) {

		List<BaseEntity> bes = null;

		bes = RulesUtils.getBaseEntitysByParentAndLinkCodeAndLinkValueWithAttributes(qwandaServiceUrl,
				this.decodedMapToken, this.token, parentCode, linkCode, linkValue, pageStart, pageSize);
		return bes;
	}

	public List<BaseEntity> getBaseEntitysByParentAndLinkCode(final String parentCode, final String linkCode,
			Integer pageStart, Integer pageSize, Boolean cache, final String stakeholderCode) {
		List<BaseEntity> bes = null;

		bes = RulesUtils.getBaseEntitysByParentAndLinkCodeWithAttributesAndStakeholderCode(qwandaServiceUrl,
				this.decodedMapToken, this.token, parentCode, linkCode, stakeholderCode);
		if (cache) {
			set("BES_" + parentCode.toUpperCase() + "_" + linkCode, bes); // WATCH THIS!!!
		}

		return bes;
	}

	public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode) {
		return this.moveBaseEntity(baseEntityCode, sourceCode, targetCode, "LNK_CORE", "LINK");
	}

	public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode, String linkCode) {
		return this.moveBaseEntity(baseEntityCode, sourceCode, targetCode, linkCode, "LINK");
	}

	public String moveBaseEntity(String baseEntityCode, String sourceCode, String targetCode, String linkCode,
			final String linkValue) {

		Link link = new Link(sourceCode, baseEntityCode, linkCode, linkValue);

		try {

			/* we call the api */
			QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/baseentitys/move/" + targetCode,
					JsonUtils.toJson(link), this.token);
		} catch (IOException e) {
			log.error(e.getMessage());
		}

		return null;
	}

	public Object getBaseEntityValue(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getObject();
		} else {
			return null;
		}
	}

	public static String getBaseEntityAttrValueAsString(BaseEntity be, String attributeCode) {

		String attributeVal = null;
		for (EntityAttribute ea : be.getBaseEntityAttributes()) {
			try {
				if (ea.getAttributeCode().equals(attributeCode)) {
					attributeVal = ea.getObjectAsString();
				}
			} catch (Exception e) {
			}
		}

		return attributeVal;
	}

	public String getBaseEntityValueAsString(final String baseEntityCode, final String attributeCode) {

		String attrValue = null;

		if (baseEntityCode != null) {

			BaseEntity be = getBaseEntityByCode(baseEntityCode);
			attrValue = getBaseEntityAttrValueAsString(be, attributeCode);
		}

		return attrValue;
	}

	public LocalDateTime getBaseEntityValueAsLocalDateTime(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDateTime();
		} else {
			return null;
		}
	}

	public LocalDate getBaseEntityValueAsLocalDate(final String baseEntityCode, final String attributeCode) {
		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueDate();
		} else {
			return null;
		}
	}

	public LocalTime getBaseEntityValueAsLocalTime(final String baseEntityCode, final String attributeCode) {

		BaseEntity be = getBaseEntityByCode(baseEntityCode);
		Optional<EntityAttribute> ea = be.findEntityAttribute(attributeCode);
		if (ea.isPresent()) {
			return ea.get().getValueTime();
		} else {
			return null;
		}
	}

	public BaseEntity getParent(String targetCode, String linkCode) {
		return this.getParent(targetCode, linkCode, null);
	}

	public BaseEntity getParent(String targetCode, String linkCode, String prefix) {

		List<BaseEntity> parents = this.getParents(targetCode, linkCode, prefix);
		if (parents != null && !parents.isEmpty()) {
			return parents.get(0);
		}

		return null;
	}

	public List<BaseEntity> getParents(final String targetCode, final String linkCode) {
		return this.getParents(targetCode, linkCode, null);
	}

	public List<BaseEntity> getParents(String targetCode, String linkCode, String prefix) {

		List<BaseEntity> parents = null;
		long sTime = System.nanoTime();
		try {

			String beJson = QwandaUtils.apiGet(this.qwandaServiceUrl + "/qwanda/entityentitys/" + targetCode
					+ "/linkcodes/" + linkCode + "/parents", this.token);
			Link[] linkArray = JsonUtils.fromJson(beJson, Link[].class);
			if (linkArray != null && linkArray.length > 0) {

				ArrayList<Link> arrayList = new ArrayList<Link>(Arrays.asList(linkArray));
				parents = new ArrayList<BaseEntity>();
				for (Link lnk : arrayList) {

					BaseEntity linkedBe = getBaseEntityByCode(lnk.getSourceCode());
					if (linkedBe != null) {

						if (prefix == null) {
							parents.add(linkedBe);
						} else {
							if (linkedBe.getCode().startsWith(prefix)) {
								parents.add(linkedBe);
							}
						}
					}
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		double difference = (System.nanoTime() - sTime) / 1e6; // get ms
		return parents;
	}

	public List<EntityEntity> getLinks(BaseEntity be) {
		return this.getLinks(be.getCode());
	}

	public List<EntityEntity> getLinks(String beCode) {

		List<EntityEntity> links = new ArrayList<EntityEntity>();
		BaseEntity be = this.getBaseEntityByCode(beCode);
		if (be != null) {

			Set<EntityEntity> linkSet = be.getLinks();
			links.addAll(linkSet);
		}

		return links;
	}

	public BaseEntity getLinkedBaseEntity(String beCode, String linkCode, String linkValue) {

		List<BaseEntity> bes = this.getLinkedBaseEntities(beCode, linkCode, linkValue);
		if (bes != null && bes.size() > 0) {
			return bes.get(0);
		}

		return null;
	}

	public List<BaseEntity> getLinkedBaseEntities(BaseEntity be) {
		return this.getLinkedBaseEntities(be.getCode(), null, null);
	}

	public List<BaseEntity> getLinkedBaseEntities(String beCode) {
		return this.getLinkedBaseEntities(beCode, null, null);
	}

	public List<BaseEntity> getLinkedBaseEntities(BaseEntity be, String linkCode) {
		return this.getLinkedBaseEntities(be.getCode(), linkCode, null);
	}

	public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode) {
		return this.getLinkedBaseEntities(beCode, linkCode, null);
	}

	public List<BaseEntity> getLinkedBaseEntities(BaseEntity be, String linkCode, String linkValue) {
		return this.getLinkedBaseEntities(be.getCode(), linkCode, linkValue);
	}

	public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode, String linkValue) {
		return this.getLinkedBaseEntities(beCode, linkCode, linkValue, 1);
	}

	public List<BaseEntity> getLinkedBaseEntities(String beCode, String linkCode, String linkValue, Integer level) {

		List<BaseEntity> linkedBaseEntities = new ArrayList<BaseEntity>();
		try {

			/* We grab all the links from the node passed as a parameter "beCode" */
			List<EntityEntity> links = this.getLinks(beCode);

			/* We loop through all the links */
			for (EntityEntity link : links) {

				if (link != null && link.getLink() != null) {

					Link entityLink = link.getLink();

					/* We get the targetCode */
					String targetCode = entityLink.getTargetCode();
					if (targetCode != null) {

						/* We use the targetCode to get the base entity */
						BaseEntity targetBe = this.getBaseEntityByCode(targetCode);
						if (targetBe != null) {

							/* If a linkCode is passed we filter using its value */
							if (linkCode != null) {
								if (entityLink.getAttributeCode() != null
										&& entityLink.getAttributeCode().equals(linkCode)) {

									/* If a linkValue is passed we filter using its value */
									if (linkValue != null) {
										if (entityLink.getLinkValue() != null
												&& entityLink.getLinkValue().equals(linkValue)) {
											linkedBaseEntities.add(targetBe);
										}
									} else {

										/* If no link value was provided we just pass the base entity */
										linkedBaseEntities.add(targetBe);
									}
								}
							} else {

								/* If not linkCode was provided we just pass the base entity */
								linkedBaseEntities.add(targetBe);
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		if (level > 1) {

			List<List<BaseEntity>> nextLevels = linkedBaseEntities.stream()
					.map(item -> this.getLinkedBaseEntities(item.getCode(), linkCode, linkValue, (level - 1)))
					.collect(Collectors.toList());
			for (List<BaseEntity> nextLevel : nextLevels) {
				linkedBaseEntities.addAll(nextLevel);
			}
		}

		return linkedBaseEntities;
	}

	public List<BaseEntity> getBaseEntityWithChildren(String beCode, Integer level) {

		if (level == 0) {
			return null; // exit point;
		}

		level--;
		BaseEntity be = this.getBaseEntityByCode(beCode);
		if (be != null) {

			List<BaseEntity> beList = new ArrayList<>();

			Set<EntityEntity> entityEntities = be.getLinks();

			// we interate through the links
			for (EntityEntity entityEntity : entityEntities) {

				Link link = entityEntity.getLink();
				if (link != null) {

					// we get the target BE
					String targetCode = link.getTargetCode();
					if (targetCode != null) {

						// recursion
						beList.addAll(this.getBaseEntityWithChildren(targetCode, level));
					}
				}
			}

			return beList;
		}

		return null;
	}

	public Boolean checkIfLinkExists(String parentCode, String linkCode, String childCode) {

		Boolean isLinkExists = false;
		QDataBaseEntityMessage dataBEMessage = QwandaUtils.getDataBEMessage(parentCode, linkCode, this.token);

		if (dataBEMessage != null) {
			BaseEntity[] beArr = dataBEMessage.getItems();

			if (beArr.length > 0) {
				for (BaseEntity be : beArr) {
					if (be.getCode().equals(childCode)) {
						isLinkExists = true;
						return isLinkExists;
					}
				}
			} else {
				isLinkExists = false;
				return isLinkExists;
			}

		}
		return isLinkExists;
	}

	/* Check If Link Exists and Available */
	public Boolean checkIfLinkExistsAndAvailable(String parentCode, String linkCode, String linkValue,
			String childCode) {
		Boolean isLinkExists = false;
		List<Link> links = getLinks(parentCode, linkCode);
		if (links != null) {
			for (Link link : links) {
				String linkVal = link.getLinkValue();

				if (linkVal != null && linkVal.equals(linkValue) && link.getTargetCode().equalsIgnoreCase(childCode)) {
					Double linkWeight = link.getWeight();
					if (linkWeight >= 1.0) {
						isLinkExists = true;
						return isLinkExists;
					}
				}
			}
		}
		return isLinkExists;
	}

	/* returns a duplicated BaseEntity from an existing beCode */
	public BaseEntity duplicateBaseEntityAttributesAndLinks(final BaseEntity oldBe, final String bePrefix,
			final String name) {

		BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
		duplicateAttributes(oldBe, newBe);
		duplicateLinks(oldBe, newBe);
		return getBaseEntityByCode(newBe.getCode());
	}

	public BaseEntity duplicateBaseEntityAttributes(final BaseEntity oldBe, final String bePrefix, final String name) {

		BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
		duplicateAttributes(oldBe, newBe);
		return getBaseEntityByCode(newBe.getCode());
	}

	public BaseEntity duplicateBaseEntityLinks(final BaseEntity oldBe, final String bePrefix, final String name) {

		BaseEntity newBe = this.create(oldBe.getCode(), bePrefix, name);
		duplicateLinks(oldBe, newBe);
		return getBaseEntityByCode(newBe.getCode());
	}

	public void duplicateAttributes(final BaseEntity oldBe, final BaseEntity newBe) {

		List<Answer> duplicateAnswerList = new ArrayList<>();

		for (EntityAttribute ea : oldBe.getBaseEntityAttributes()) {
			duplicateAnswerList.add(new Answer(newBe.getCode(), newBe.getCode(), ea.getAttributeCode(), ea.getValue()));
		}

		this.saveAnswers(duplicateAnswerList);
	}

	public void duplicateLinks(final BaseEntity oldBe, final BaseEntity newBe) {
		for (EntityEntity ee : oldBe.getLinks()) {
			createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
					ee.getLink().getLinkValue(), ee.getLink().getWeight());
		}
	}

	public void duplicateLink(final BaseEntity oldBe, final BaseEntity newBe, final BaseEntity childBe) {
		for (EntityEntity ee : oldBe.getLinks()) {
			if (ee.getLink().getTargetCode() == childBe.getCode()) {

				createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
						ee.getLink().getLinkValue(), ee.getLink().getWeight());
				break;
			}
		}
	}

	public void duplicateLinksExceptOne(final BaseEntity oldBe, final BaseEntity newBe, String linkValue) {
		for (EntityEntity ee : oldBe.getLinks()) {
			if (ee.getLink().getLinkValue() == linkValue) {
				continue;
			}
			createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
					ee.getLink().getLinkValue(), ee.getLink().getWeight());
		}
	}

	public BaseEntity cloneBeg(final BaseEntity oldBe, final BaseEntity newBe, final BaseEntity childBe,
			String linkValue) {
		duplicateLinksExceptOne(oldBe, newBe, linkValue);
		duplicateLink(oldBe, newBe, childBe);
		return getBaseEntityByCode(newBe.getCode());
	}

	/* clones links of oldBe to newBe from supplied arraylist linkValues */
	public BaseEntity copyLinks(final BaseEntity oldBe, final BaseEntity newBe, final String[] linkValues) {
		log.info("linkvalues   ::   " + Arrays.toString(linkValues));
		for (EntityEntity ee : oldBe.getLinks()) {
			log.info("old be linkValue   ::   " + ee.getLink().getLinkValue());
			for (String linkValue : linkValues) {
				log.info("a linkvalue   ::   " + linkValue);
				if (ee.getLink().getLinkValue().equals(linkValue)) {
					createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
							ee.getLink().getLinkValue(), ee.getLink().getWeight());
					log.info("creating link for   ::   " + linkValue);
				}
			}
		}
		return getBaseEntityByCode(newBe.getCode());
	}

	/*
	 * clones all links of oldBe to newBe except the linkValues supplied in
	 * arraylist linkValues
	 */
	public BaseEntity copyLinksExcept(final BaseEntity oldBe, final BaseEntity newBe, final String[] linkValues) {
		log.info("linkvalues   ::   " + Arrays.toString(linkValues));
		for (EntityEntity ee : oldBe.getLinks()) {
			log.info("old be linkValue   ::   " + ee.getLink().getLinkValue());
			for (String linkValue : linkValues) {
				log.info("a linkvalue   ::   " + linkValue);
				if (ee.getLink().getLinkValue().equals(linkValue)) {
					continue;
				}
				createLink(newBe.getCode(), ee.getLink().getTargetCode(), ee.getLink().getAttributeCode(),
						ee.getLink().getLinkValue(), ee.getLink().getWeight());
				log.info("creating link for   ::   " + linkValue);
			}
		}
		return getBaseEntityByCode(newBe.getCode());
	}

	public void updateBaseEntityStatus(BaseEntity be, String status) {
		this.updateBaseEntityStatus(be.getCode(), status);
	}

	public void updateBaseEntityStatus(String beCode, String status) {

		String attributeCode = "STA_STATUS";
		this.updateBaseEntityAttribute(beCode, beCode, attributeCode, status);
	}

	public void updateBaseEntityStatus(BaseEntity be, String userCode, String status) {
		this.updateBaseEntityStatus(be.getCode(), userCode, status);
	}

	public void updateBaseEntityStatus(String beCode, String userCode, String status) {

		String attributeCode = "STA_" + userCode;
		this.updateBaseEntityAttribute(userCode, beCode, attributeCode, status);

		/* new status for v3 */
		switch (status) {
		case "green":
		case "red":
		case "orange":
		case "yellow": {

			BaseEntity be = this.getBaseEntityByCode(beCode);
			if (be != null) {

				String attributeCodeStatus = "STA_" + status.toUpperCase();
				String existingValueArray = be.getValue(attributeCodeStatus, "[]");
				JsonParser jsonParser = new JsonParser();
				JsonArray existingValues = jsonParser.parse(existingValueArray).getAsJsonArray();
				existingValues.add(userCode);
				this.saveAnswer(new Answer(beCode, beCode, attributeCodeStatus, JsonUtils.toJson(existingValues)));
			}
		}
		default: {
		}
		}
	}

	public void updateBaseEntityStatus(BaseEntity be, List<String> userCodes, String status) {
		this.updateBaseEntityStatus(be.getCode(), userCodes, status);
	}

	public void updateBaseEntityStatus(String beCode, List<String> userCodes, String status) {

		for (String userCode : userCodes) {
			this.updateBaseEntityStatus(beCode, userCode, status);
		}
	}

	public List<Link> getLinks(final String parentCode, final String linkCode) {
		List<Link> links = RulesUtils.getLinks(this.qwandaServiceUrl, this.decodedMapToken, this.token, parentCode,
				linkCode);
		return links;
	}

	public String updateBaseEntity(BaseEntity be) {
		try {
			VertxUtils.writeCachedJson(getRealm(),be.getCode(), JsonUtils.toJson(be));
			return QwandaUtils.apiPutEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(be),
					this.token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String saveSearchEntity(SearchEntity se) { // TODO: Ugly
		String ret = null;
		try {
			if (se != null) {
				if (!se.hasCode()) {
					log.error("ERROR! searchEntity se has no code!");
				}
				if (se.getId() == null) {
					BaseEntity existing = VertxUtils.readFromDDT(getRealm(),se.getCode(), this.realm);
					if (existing != null) {
						se.setId(existing.getId());
					}
				}
				VertxUtils.writeCachedJson(getRealm(),se.getCode(), JsonUtils.toJson(se));
				if (se.getId() != null) {
					ret = QwandaUtils.apiPutEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(se),
							this.token);

				} else {
					ret = QwandaUtils.apiPostEntity(this.qwandaServiceUrl + "/qwanda/baseentitys", JsonUtils.toJson(se),
							this.token);
				}
				saveBaseEntityAttributes(se);
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return ret;
	}

	public void saveBaseEntityAttributes(BaseEntity be) {
		if ((be == null)||(be.getCode()==null) ) {
			throw new NullPointerException("Cannot save be because be is null or be.getCode is null");
		}
		List<Answer> answers = new ArrayList<Answer>();

		for (EntityAttribute ea : be.getBaseEntityAttributes()) {
			Answer attributeAnswer = new Answer(be.getCode(), be.getCode(), ea.getAttributeCode(), ea.getAsString());
			attributeAnswer.setChangeEvent(false);
			answers.add(attributeAnswer);
		}
		this.saveAnswers(answers);
	}

	public BaseEntity updateCachedBaseEntity(final Answer answer) {
		BaseEntity cachedBe = this.getBaseEntityByCode(answer.getTargetCode());
		// Add an attribute if not already there
		try {
			String attributeCode = answer.getAttributeCode();
			if (!attributeCode.startsWith("RAW_")) {
				Attribute attribute = null;

				if (RulesUtils.attributeMap != null) {
					attribute = RulesUtils.attributeMap.get(attributeCode);

					if (attribute != null) {
						answer.setAttribute(attribute);
					}

					if (answer.getAttribute() == null || cachedBe == null) {
						log.info("Null Attribute or null BE , targetCode=["+answer.getTargetCode()+"]");
					} else {
						cachedBe.addAnswer(answer);
					}
					VertxUtils.writeCachedJson(getRealm(),answer.getTargetCode(), JsonUtils.toJson(cachedBe));
				}
			}

			/*
			 * answer.setAttribute(RulesUtils.attributeMap.get(answer.getAttributeCode()));
			 * if (answer.getAttribute() == null) { log.info("Null Attribute"); }
			 * else cachedBe.addAnswer(answer);
			 * VertxUtils.writeCachedJson(answer.getTargetCode(),
			 * JsonUtils.toJson(cachedBe));
			 * 
			 */
		} catch (BadDataException e) {
			e.printStackTrace();
		}
		return cachedBe;
	}

	public BaseEntity updateCachedBaseEntity(List<Answer> answers) {
		Answer firstanswer = null;
		if ((answers != null)&&(!answers.isEmpty())) {
				firstanswer = answers.get(0);
		} else {
			throw new NullPointerException("Answers cannot be null or empty for updateCacheBaseEntity");
		}
		BaseEntity cachedBe = null;

		if (firstanswer != null) {
			if (firstanswer.getTargetCode() == null) {
				throw new NullPointerException("firstanswer getTargetCode cannot be null for updateCacheBaseEntity");
			}
			log.info("firstAnswer.targetCode="+firstanswer.getTargetCode());
			cachedBe = this.getBaseEntityByCode(firstanswer.getTargetCode());
		} else {
			return null;
		}
		
		if (cachedBe != null) {
			if ((cachedBe == null ) || (cachedBe.getCode()==null)) {
				throw new NullPointerException("cachedBe.getCode cannot be null for updateCacheBaseEntity , targetCode="+firstanswer.getTargetCode()+" cacheBe=["+cachedBe);
			}
		} else {
			throw new NullPointerException("cachedBe cannot be null for updateCacheBaseEntity , targetCode="+firstanswer.getTargetCode());

		}

		for (Answer answer : answers) {

			if (!answer.getAttributeCode().startsWith("RAW_")) {

				// Add an attribute if not already there
				try {
					if (answer.getAttribute() == null) {
						Attribute attribute = RulesUtils.getAttribute(answer.getAttributeCode(), token);

						if (attribute != null) {
							answer.setAttribute(attribute);
						}
					}
					if (answer.getAttribute() == null) {
						continue;
					}
					cachedBe.addAnswer(answer);
				} catch (BadDataException e) {
					e.printStackTrace();
				}
			}
		}

			VertxUtils.writeCachedJson(getRealm(),cachedBe.getCode(), JsonUtils.toJson(cachedBe));


		return cachedBe;
	}

	public Link createLink(String sourceCode, String targetCode, String linkCode, String linkValue, Double weight) {

		System.out
				.println("CREATING LINK between " + sourceCode + "and" + targetCode + "with LINK VALUE = " + linkValue);
		Link link = new Link(sourceCode, targetCode, linkCode, linkValue);
		link.setWeight(weight);
		try {
			BaseEntity source = this.getBaseEntityByCode(sourceCode);
			BaseEntity target = this.getBaseEntityByCode(targetCode);
			Attribute linkAttribute = new AttributeLink(linkCode, linkValue);
			try {
				source.addTarget(target, linkAttribute, weight, linkValue);
				this.updateBaseEntity(source);

				QwandaUtils.apiPostEntity(qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link),
						this.token);
			} catch (BadDataException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return link;
	}

	public Link updateLink(String sourceCode, String targetCode, String linkCode, String linkValue, Double weight) {

		System.out
				.println("UPDATING LINK between " + sourceCode + "and" + targetCode + "with LINK VALUE = " + linkValue);
		Link link = new Link(sourceCode, targetCode, linkCode, linkValue);
		link.setWeight(weight);
		try {
			BaseEntity source = this.getBaseEntityByCode(sourceCode);
			BaseEntity target = this.getBaseEntityByCode(targetCode);
			Attribute linkAttribute = new AttributeLink(linkCode, linkValue);
			source.addTarget(target, linkAttribute, weight, linkValue);
			this.updateBaseEntity(source);

			QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/links", JsonUtils.toJson(link), this.token);
		} catch (IOException | BadDataException e) {
			e.printStackTrace();
		}
		return link;
	}

	public Link updateLink(String groupCode, String targetCode, String linkCode, Double weight) {

		log.info("UPDATING LINK between " + groupCode + "and" + targetCode);
		Link link = new Link(groupCode, targetCode, linkCode);
		link.setWeight(weight);
		try {
			QwandaUtils.apiPutEntity(qwandaServiceUrl + "/qwanda/links", JsonUtils.toJson(link), this.token);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return link;
	}

	public Link[] getUpdatedLink(String parentCode, String linkCode) {
		List<Link> links = this.getLinks(parentCode, linkCode);
		Link[] items = new Link[links.size()];
		items = (Link[]) links.toArray(items);
		return items;
	}

	/*
	 * Gets all the attribute and their value for the given basenentity code
	 */
	public Map<String, String> getMapOfAllAttributesValuesForBaseEntity(String beCode) {

		BaseEntity be = this.getBaseEntityByCode(beCode);
		log.info("The load is ::" + be);
		Set<EntityAttribute> eaSet = be.getBaseEntityAttributes();
		log.info("The set of attributes are  :: " + eaSet);
		Map<String, String> attributeValueMap = new HashMap<String, String>();
		for (EntityAttribute ea : eaSet) {
			String attributeCode = ea.getAttributeCode();
			log.info("The attribute code  is  :: " + attributeCode);
			String value = ea.getAsLoopString();
			attributeValueMap.put(attributeCode, value);
		}

		return attributeValueMap;
	}

	public List getLinkList(String groupCode, String linkCode, String linkValue, String token) {

		// String qwandaServiceUrl = "http://localhost:8280";
		String qwandaServiceUrl = System.getenv("REACT_APP_QWANDA_API_URL");
		List linkList = null;

		try {
			String attributeString = QwandaUtils.apiGet(qwandaServiceUrl + "/qwanda/entityentitys/" + groupCode
					+ "/linkcodes/" + linkCode + "/children/" + linkValue, token);
			if (attributeString != null) {
				linkList = JsonUtils.fromJson(attributeString, List.class);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return linkList;

	}

	/*
	 * Returns only non-hidden links or all the links based on the includeHidden
	 * value
	 */
	public List getLinkList(String groupCode, String linkCode, String linkValue, Boolean includeHidden) {

		// String qwandaServiceUrl = "http://localhost:8280";
		BaseEntity be = getBaseEntityByCode(groupCode);
		List<Link> links = getLinks(groupCode, linkCode);

		List linkList = null;

		if (links != null) {
			for (Link link : links) {
				String linkVal = link.getLinkValue();

				if (linkVal != null && linkVal.equals(linkValue)) {
					Double linkWeight = link.getWeight();
					if (!includeHidden) {
						if (linkWeight >= 1.0) {
							linkList.add(link);

						}
					} else {
						linkList.add(link);
					}
				}
			}
		}
		return linkList;

	}

	/*
	 * Sorting Columns of a SearchEntity as per the weight in either Ascending or
	 * descending order
	 */
	public List<String> sortEntityAttributeBasedOnWeight(final List<EntityAttribute> ea, final String sortOrder) {

		if (ea.size() > 1) {
			Collections.sort(ea, new Comparator<EntityAttribute>() {

				@Override
				public int compare(EntityAttribute ea1, EntityAttribute ea2) {
					if (ea1.getWeight() != null && ea2.getWeight() != null) {
						if (sortOrder.equalsIgnoreCase("ASC"))
							return (ea1.getWeight()).compareTo(ea2.getWeight());
						else
							return (ea2.getWeight()).compareTo(ea1.getWeight());

					} else
						return 0;
				}
			});
		}

		List<String> searchHeader = new ArrayList<String>();
		for (EntityAttribute ea1 : ea) {
			searchHeader.add(ea1.getAttributeCode().substring("COL_".length()));
		}

		return searchHeader;
	}

	public BaseEntity baseEntityForLayout(String realm, String token, Layout layout) {

		if (layout.getPath() == null) {
			return null;
		}

		String serviceToken = RulesUtils.generateServiceToken(realm);
		if (serviceToken != null) {

			BaseEntity beLayout = null;

			/* we check if the baseentity for this layout already exists */
			// beLayout =
			// RulesUtils.getBaseEntityByAttributeAndValue(RulesUtils.qwandaServiceUrl,
			// this.decodedTokenMap, this.token, "PRI_LAYOUT_URI", layout.getPath());
			String precode = String.valueOf(layout.getPath().replaceAll("[^a-zA-Z0-9]", "").toUpperCase().hashCode());
			String layoutCode = ("LAY_" + realm + "_" + precode).toUpperCase();

			log.info("Layout - Handling " + layoutCode);
			try {
				// Check if in cache first to save time.
				beLayout = VertxUtils.readFromDDT(getRealm(),layoutCode, serviceToken);
				if (beLayout==null) {
					beLayout = QwandaUtils.getBaseEntityByCode(layoutCode, serviceToken);
					if (beLayout != null) {
						VertxUtils.writeCachedJson(layoutCode, JsonUtils.toJson(beLayout), serviceToken);
					}
				}

			} catch (IOException e) {
				log.error(e.getMessage());
			}

			/* if the base entity does not exist, we create it */
			if (beLayout == null) {

				log.info("Layout - Creating base entity " + layoutCode);

				/* otherwise we create it */
				beLayout = this.create(layoutCode, layout.getName());
			}

			if (beLayout != null) {

				log.info("Layout - Creating base entity " + layoutCode);

				/* otherwise we create it */
				beLayout = this.create(layoutCode, layout.getName());
			}

			if (beLayout != null) {

				this.addAttributes(beLayout);

				/*
				 * we get the modified time stored in the BE and we compare it to the layout one
				 */
				String beModifiedTime = beLayout.getValue("PRI_LAYOUT_MODIFIED_DATE", null);
				
				log.debug("*** match layout mod date ["+layout.getModifiedDate()+"] with be layout ["+beModifiedTime);

				/* if the modified time is not the same, we update the layout BE */
				/* setting layout attributes */
				List<Answer> answers = new ArrayList<>();

				/* download the content of the layout */
				String content = LayoutUtils.downloadLayoutContent(layout);

				log.debug("layout.getData().hashcode()="+layout.getData().trim().hashCode());

				log.debug("content.hashcode()="+content.trim().hashCode());


				Optional<EntityAttribute> primaryLayoutData = beLayout.findEntityAttribute("PRI_LAYOUT_DATA");
				String beData = null;
				if(primaryLayoutData.isPresent()) {
					log.debug("beLayout.findEntityAttribute(\"PRI_LAYOUT_DATA\").get().getAsString().trim().hashcode()="+beLayout.findEntityAttribute("PRI_LAYOUT_DATA").get().getAsString().trim().hashCode());
					beData = beLayout.findEntityAttribute("PRI_LAYOUT_DATA").get().getAsString().trim();
				}
				
				if (true/*!layout.getData().trim().equals(beData)*/
						
						) {
					log.info("Resaving layout: " + layoutCode);


				Answer newAnswerContent = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_DATA",
						content);

				newAnswerContent.setChangeEvent(true);
				answers.add(newAnswerContent);

				Answer newAnswer = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_URI",
						layout.getPath());
				answers.add(newAnswer);

				Answer newAnswer2 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_URL",
						layout.getDownloadUrl());
				answers.add(newAnswer2);

				Answer newAnswer3 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_NAME",
						layout.getName());
				answers.add(newAnswer3);

				Answer newAnswer4 = new Answer(beLayout.getCode(), beLayout.getCode(), "PRI_LAYOUT_MODIFIED_DATE",
						layout.getModifiedDate());
				answers.add(newAnswer4);

				this.saveAnswers(answers);

				/* create link between GRP_LAYOUTS and this new LAY_XX base entity */
				this.createLink("GRP_LAYOUTS", beLayout.getCode(), "LNK_CORE", "LAYOUT", 1.0);
				} else {
					log.info("Already have same layout data - not saving ");
				}
			}

			return beLayout;
		}

		return null;
	}

	/*
	 * copy all the attributes from one BE to another BE sourceBe : FROM targetBe :
	 * TO
	 */
	public BaseEntity copyAttributes(final BaseEntity sourceBe, final BaseEntity targetBe) {

		Map<String, String> map = new HashMap<>();
		map = getMapOfAllAttributesValuesForBaseEntity(sourceBe.getCode());
		RulesUtils.ruleLogger("MAP DATA   ::   ", map);

		List<Answer> answers = new ArrayList<Answer>();
		try {
			for (Map.Entry<String, String> entry : map.entrySet()) {
				Answer answerObj = new Answer(sourceBe.getCode(), targetBe.getCode(), entry.getKey(), entry.getValue());
				answers.add(answerObj);
			}
			saveAnswers(answers);
		} catch (Exception e) {
		}

		return getBaseEntityByCode(targetBe.getCode());
	}

	public String removeLink(final String parentCode, final String childCode, final String linkCode) {
		Link link = new Link(parentCode, childCode, linkCode);
		try {
			return QwandaUtils.apiDelete(this.qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link),
					this.token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/* Remove link with specific link Value */
	public String removeLink(final String parentCode, final String childCode, final String linkCode,
			final String linkValue) {
		Link link = new Link(parentCode, childCode, linkCode, linkValue);
		try {
			return QwandaUtils.apiDelete(this.qwandaServiceUrl + "/qwanda/entityentitys", JsonUtils.toJson(link),
					this.token);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*
	 * Returns comma seperated list of all the childcode for the given parent code
	 * and the linkcode
	 */
	public String getAllChildCodes(final String parentCode, final String linkCode) {
		String childs = null;
		List<String> childBECodeList = new ArrayList<String>();
		List<BaseEntity> childBE = this.getLinkedBaseEntities(parentCode, linkCode);
		if (childBE != null) {
			for (BaseEntity be : childBE) {
				childBECodeList.add(be.getCode());
			}
			childs = "\"" + String.join("\", \"", childBECodeList) + "\"";
			childs = "[" + childs + "]";
		}

		return childs;
	}

	/* Get array String value from an attribute of the BE */
	public List<String> getBaseEntityAttrValueList(BaseEntity be, String attributeCode) {

		String myLoadTypes = be.getValue(attributeCode, null);

		if (myLoadTypes != null) {
			List<String> loadTypesList = new ArrayList<String>();
			/* Removing brackets "[]" and double quotes from the strings */
			String trimmedStr = myLoadTypes.substring(1, myLoadTypes.length() - 1).toString().replaceAll("\"", "");
			if (trimmedStr != null && !trimmedStr.isEmpty()) {
				loadTypesList = Arrays.asList(trimmedStr.split("\\s*,\\s*"));
				return loadTypesList;
			} else {
				return null;
			}
		} else
			return null;
	}

	public QBulkPullMessage createQBulkPullMessage(QBulkMessage msg) {

		UUID uuid = UUID.randomUUID();
		QBulkPullMessage pullMsg = new QBulkPullMessage(uuid.toString());

		// Put the QBulkMessage into the PontoonDDT

		// then create the QBulkPullMessage
		pullMsg.setPullUrl(GennySettings.pontoonUrl + "/" + uuid);
		return pullMsg;

	}

	public static QBulkPullMessage createQBulkPullMessage(JsonObject msg) {

		UUID uuid = UUID.randomUUID();

		QBulkPullMessage pullMsg = new QBulkPullMessage(uuid.toString());

		// Put the QBulkMessage into the PontoonDDT
		DistMap.getDistPontoonBE(getRealm()).put(uuid, msg, 2, TimeUnit.MINUTES);

		// then create the QBulkPullMessage
		pullMsg.setPullUrl(GennySettings.pontoonUrl + "/pull/" + uuid);
		return pullMsg;

	}
}
