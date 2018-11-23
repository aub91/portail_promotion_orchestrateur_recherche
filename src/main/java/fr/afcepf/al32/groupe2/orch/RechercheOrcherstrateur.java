package fr.afcepf.al32.groupe2.orch;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.afcepf.al32.groupe2.ws.dto.PromotionDto;
import fr.afcepf.al32.groupe2.ws.dto.ResponseGeoApiDto;
import fr.afcepf.al32.groupe2.ws.dto.ResponseWsDto;
import fr.afcepf.al32.groupe2.ws.dto.SearchByCategoryResponseDto;
import fr.afcepf.al32.groupe2.ws.dto.SearchByKeywordsResponseDto;
import fr.afcepf.al32.groupe2.ws.dto.SearchByShopResponseDto;
import fr.afcepf.al32.groupe2.ws.dto.ShopDto;

@RestController
@RequestMapping(value = "/orch", headers = "Accept=application/json")
public class RechercheOrcherstrateur {

	private RestTemplate restTemplate;
	private String base_url_rechercheGeo, base_url_validationGeo, base_url_recherche = null;
	// "http://localhost:8082/wsRechercheGeo/rest/rechercheGeo/commerce?source=montrouge&perimetre=10

	public RechercheOrcherstrateur() {
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

	public List<PromotionDto> searchPromotion(@RequestParam("source") String source,
			@RequestParam("perimetre") Integer perimetre, @RequestParam("mots") List<String> mots,
			@RequestParam("categorie") Long id) {

		final boolean estUneRechercheDeGeoApi = (source != null && !source.isEmpty())
				&& (perimetre != null && perimetre != 0);
		boolean adresseValide = false;
		ResponseWsDto responseWsDto = null;
		List<ShopDto> shopDtos;
		List<PromotionDto> promotionDtosbyGeoRecherche = null;

		if (estUneRechercheDeGeoApi) {
			base_url_validationGeo += "?source=" + source;
			ResponseGeoApiDto geoApiDto = restTemplate.getForObject(base_url_validationGeo, ResponseGeoApiDto.class);

			adresseValide = geoApiDto.getStatus() == "OK";
			if (adresseValide) {
				base_url_rechercheGeo += "?source=" + source + "&perimetre=" + perimetre;
				responseWsDto = restTemplate.getForObject(base_url_rechercheGeo, ResponseWsDto.class);
				shopDtos = responseWsDto.getListDtos();
				promotionDtosbyGeoRecherche = traitementByShop(shopDtos);
			}
		}

		List<PromotionDto> listeFinale = null;

		if (null == mots && (null == id || id == 0l)) {
			listeFinale = promotionDtosbyGeoRecherche;
		} else if (null == mots && (null != id || id != 0l)) {
			listeFinale = traitementByCategory(id, adresseValide, promotionDtosbyGeoRecherche);
		} else if (null != mots && (null != id || id != 0l)) {
			listeFinale = traitementByCategoryEtMotCles(id, mots, adresseValide, promotionDtosbyGeoRecherche);
		} else if (null != mots && (null == id || id == 0l)) {
			listeFinale = traitementByKeyWords(mots, adresseValide, promotionDtosbyGeoRecherche);
		} else {
			System.out.println("cas pas g�r�");
		}

		return listeFinale;
	}

	private List<PromotionDto> traitementByCategoryEtMotCles(Long id, List<String> mots, boolean adresseValide,
			List<PromotionDto> promotionDtosbyGeoRecherche) {

		List<PromotionDto> listeFinale = traitementByCategory(id, adresseValide, promotionDtosbyGeoRecherche);
		List<PromotionDto> listeMots = traitementByKeyWords(mots, adresseValide, promotionDtosbyGeoRecherche);
		listeMots.retainAll(promotionDtosbyGeoRecherche);
		listeFinale.retainAll(listeMots);

		return listeFinale;
	}

	private List<PromotionDto> traitementByShop(List<ShopDto> shopDtos) {

		ObjectMapper mapper = new ObjectMapper();
		StringJoiner joiner = new StringJoiner(",", "[", "]");
		shopDtos.stream().map(shop -> {
			try {
				return mapper.writeValueAsString(shop);
			} catch (JsonProcessingException e) {
				return "";
			}
		}).forEach(joiner::add);

		String url_byShop = base_url_recherche + "/byShop";
		String requestJson = joiner.toString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
		SearchByShopResponseDto searchByShopResponseDto = restTemplate.postForObject(url_byShop, entity,
				SearchByShopResponseDto.class);

		return searchByShopResponseDto.getPromotionsDto();

	}

	private List<PromotionDto> traitementByCategory(Long id, boolean adresseValide,
			List<PromotionDto> promotionDtosbyGeoRecherche) {
		List<PromotionDto> listeFinale = null;

		String url_byCategory = base_url_recherche + "/byCategory";
		String requestJson = id.toString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
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
		List<PromotionDto> listeFinale;
		ObjectMapper mapper = new ObjectMapper();

		StringJoiner joiner = new StringJoiner(",", "[", "]");
		mots.stream().map(mot -> {
			try {
				return mapper.writeValueAsString(mot);
			} catch (JsonProcessingException e) {
				return "";
			}
		}).forEach(joiner::add);

		String url_Keywords = base_url_recherche + "/byKeywords";
		String requestJson = joiner.toString();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
		SearchByKeywordsResponseDto searchByKeywordsResponseDto = restTemplate.postForObject(url_Keywords, entity,
				SearchByKeywordsResponseDto.class);
		listeFinale = searchByKeywordsResponseDto.getPromotionsDto();
		if (adresseValide) {
			listeFinale.retainAll(promotionDtosbyGeoRecherche);
		}
		return listeFinale;
	}

}
