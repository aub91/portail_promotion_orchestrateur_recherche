package fr.afcepf.al32.groupe2.orch;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import fr.afcepf.al32.groupe2.ws.dto.PromotionDto;
import fr.afcepf.al32.groupe2.ws.dto.ResponseGeoApiDto;
import fr.afcepf.al32.groupe2.ws.dto.ResponseWsDto;


public class RechercheOrcherstrateur {
	
	private RestTemplate restTemplate;
	private String  base_url_rechercheGeo,base_url_validationGeo, base_url_recherche = null;
	//"http://localhost:8082/wsRechercheGeo/rest/rechercheGeo/commerce?source=montrouge&perimetre=10
	


	public RechercheOrcherstrateur() {
		restTemplate = new RestTemplate();
		try {
			Properties props = new Properties();
			InputStream is = Thread.currentThread().getContextClassLoader()
					               .getResourceAsStream("ws_recherche.properties");
			props.load(is);
			is.close();
			
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
			@RequestParam("perimetre") Integer perimetre, @RequestParam("mots") String[] mots,
			@RequestParam("categorie") Long id) {

		final boolean estUneRechercheDeGeoApi = (source != null && !source.isEmpty())
				&& (perimetre != null && perimetre != 0);
		boolean adresseValide;
		if (estUneRechercheDeGeoApi) {
			base_url_validationGeo += "?source=" + source;
			ResponseGeoApiDto geoApiDto =restTemplate.getForObject(base_url_validationGeo, ResponseGeoApiDto.class);
			
			adresseValide = geoApiDto.getStatus() == "OK";
			if (adresseValide) {
				base_url_rechercheGeo +="?source=" + source + "&perimetre=" + perimetre;
				ResponseWsDto responseWsDto = restTemplate.getForObject(base_url_rechercheGeo,ResponseWsDto.class );				
			}
			
		}
		
		

		return null;
	}

}
