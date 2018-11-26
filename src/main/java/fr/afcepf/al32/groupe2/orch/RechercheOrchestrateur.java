package fr.afcepf.al32.groupe2.orch;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

import fr.afcepf.al32.groupe2.ws.dto.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.text.html.parser.Entity;

@RestController
@RequestMapping(value = "/search", headers = "Accept=application/json")
public class RechercheOrchestrateur {

	private RestTemplate restTemplate;
	private String base_url_rechercheGeo, base_url_validationGeo, base_url_recherche = null;
	// "http://localhost:8082/wsRechercheGeo/rest/rechercheGeo/commerce?source=montrouge&perimetre=10

	public RechercheOrchestrateur() {
		restTemplate = new RestTemplate();
		try {
			Properties props = new Properties();
			InputStream inputStream = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("ws_recherche.properties");
			props.load(inputStream);
			inputStream.close();

			this.base_url_rechercheGeo = props.getProperty("ws_recherche.base_url_rechercheGeo");
			this.base_url_validationGeo = props.getProperty("ws_recherche.base_url_validationGeo");
			this.base_url_recherche = props.getProperty("ws_recherche.base_url_recherche");

			System.out.println(base_url_validationGeo);
			System.out.println(base_url_recherche);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@GetMapping("")
	public OrchestratorResearchDtoResponse searchPromotion(@RequestParam("source") String source,
														   @RequestParam("perimetre") Integer perimetre, @RequestParam("mots") List<String> mots,
														   @RequestParam("categorie") Long categoryId) {

		final boolean estUneRechercheDeGeoApi = (source != null && !source.isEmpty())
				&& (perimetre != null && perimetre > 0);
		boolean adresseValide = false;
		ResponseWsDto responseWsDto = null;
		List<ShopDto> shopDtos;
		List<PromotionDto> promotionDtosbyGeoRecherche = null;

		if (estUneRechercheDeGeoApi) {
			String urlValidation = base_url_validationGeo + "?source=" + source;
			ResponseGeoApiDto geoApiDto = restTemplate.getForObject(urlValidation, ResponseGeoApiDto.class);

			adresseValide = geoApiDto.getStatus().equals("OK");
			if (adresseValide) {
				String urlRecherche = base_url_rechercheGeo + "?source=" + source + "&perimetre=" + perimetre;
				responseWsDto = restTemplate.getForObject(urlRecherche, ResponseWsDto.class);
				shopDtos = responseWsDto.getListDtos();
				promotionDtosbyGeoRecherche = traitementByShop(shopDtos);
			}
		}

		List<PromotionDto> listeFinale = null;

		if (categoryId != null && categoryId != 0L) {
			listeFinale = traitementByCategoryEtMotCles(categoryId, mots, adresseValide, promotionDtosbyGeoRecherche);
		} else {
			listeFinale = traitementByKeyWords(mots, adresseValide, promotionDtosbyGeoRecherche);
		}

		return new OrchestratorResearchDtoResponse(categoryId, mots, source, perimetre, listeFinale, adresseValide);
	}

	private List<PromotionDto> traitementByCategoryEtMotCles(Long id, List<String> mots, boolean adresseValide,
			List<PromotionDto> promotionDtosbyGeoRecherche) {

		List<PromotionDto> listeFinale = null;

		String url_Keywords = base_url_recherche + "/byCategoryAndKeywords";

		HttpEntity<String> entity = null;
		try {
			entity = constructRequestBody(new CategoryAndKeywordsDto(new CategoryProductDto(id), mots));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		SearchByCategoryAndKeywordsResponseDto searchByCategoryAndKeywordsResponseDto = restTemplate.postForObject(url_Keywords, entity,
				SearchByCategoryAndKeywordsResponseDto.class);
		listeFinale = searchByCategoryAndKeywordsResponseDto.getPromotionsDto();

		if (adresseValide) {
			listeFinale.retainAll(promotionDtosbyGeoRecherche);
		}

		return listeFinale;
	}

	private List<PromotionDto> traitementByShop(List<ShopDto> shopDtos) {

		String url_byShop = base_url_recherche + "/byShop";
		HttpEntity<String> entity = null;
		try {
			entity = constructRequestBody(shopDtos);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		SearchByShopResponseDto searchByShopResponseDto = restTemplate.postForObject(url_byShop, entity,
				SearchByShopResponseDto.class);

		return searchByShopResponseDto.getPromotionsDto();

	}

	private List<PromotionDto> traitementByCategory(Long id, boolean adresseValide,
			List<PromotionDto> promotionDtosbyGeoRecherche) {
		List<PromotionDto> listeFinale;

		String url_byCategory = base_url_recherche + "/byCategory";

		HttpEntity<String> entity = null;

		try {
			entity = constructRequestBody(new CategoryProductDto(id));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		SearchByCategoryResponseDto searchByCategoryResponseDto = restTemplate.postForObject(url_byCategory, entity,
				SearchByCategoryResponseDto.class);
		listeFinale = searchByCategoryResponseDto.getPromotionsDto();

		if (adresseValide) {
			listeFinale.retainAll(promotionDtosbyGeoRecherche);
		}
		return listeFinale;
	}

	private List<PromotionDto> traitementByKeyWords(List<String> mots, boolean adresseValide,
			List<PromotionDto> promotionDtosbyGeoRecherche) {
		List<PromotionDto> listeFinale = null;

		String url_Keywords = base_url_recherche + "/byKeywords";

		HttpEntity<String> entity = null;
		try {
			entity = constructRequestBody(mots);

		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		SearchByKeywordsResponseDto searchByKeywordsResponseDto = restTemplate.postForObject(url_Keywords, entity,
				SearchByKeywordsResponseDto.class);
		listeFinale = searchByKeywordsResponseDto.getPromotionsDto();

		if (adresseValide) {
			listeFinale.retainAll(promotionDtosbyGeoRecherche);
		}
		return listeFinale;
	}

	private HttpEntity<String> constructRequestBody(Object valueToJsonify) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String requestJson = mapper.writeValueAsString(valueToJsonify);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(requestJson, headers);
	}

}
