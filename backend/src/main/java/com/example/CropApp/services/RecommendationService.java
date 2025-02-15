package com.example.CropApp.services;

import com.example.CropApp.entities.Recommendation;
import com.example.CropApp.entities.User;
import com.example.CropApp.repositories.RecommendationRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Service
public class RecommendationService {

    private final RestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    public RecommendationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Transactional
    public Map<String, Object> generateRecommendation(Map<String, Object> inputData, String jwt) throws Exception {
        String flaskApiUrl = "http://localhost:5000/crop-predict"; // URL de l'API Flask

        // Configurer les en-têtes pour la requête HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Préparer la requête avec les données directement sous forme de JSON
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(inputData, headers);

        // Effectuer l'appel à l'API Flask et récupérer la réponse brute
        ResponseEntity<Map> response = restTemplate.postForEntity(flaskApiUrl, request, Map.class);

        System.out.println(response.getBody());
        if (response != null && response.getBody() != null) {
           // Map<String, Object> responseBody = response.getBody();
            Map<String, Object> formData = (Map<String, Object>) inputData.get("formdata");


            // Extraction des données à partir de la structure JSON retournée
            //List<?> resultList = (List<?>) responseBody.get("chart_data");



           /* if (resultList == null || resultList.size() < 4) {
                throw new RuntimeException("Invalid structure in Flask response.");
            }

            // Extraction des valeurs spécifiques
            String recommendedCrop = ((List<String>) resultList.get(0)).get(0); // Le crop recommandé
            Double temperature = Double.valueOf(resultList.get(1).toString());
            Double humidity = Double.valueOf(resultList.get(2).toString());
            Double rainfall = Double.valueOf(resultList.get(3).toString());
*/



            Map<String, Object> responseBody = response.getBody();


            Map<String, Object> responsee = (Map<String, Object>) responseBody.get("response");

            Map<String, Object> result = (Map<String, Object>) responsee.get("result");



// Extraction des données à partir de la structure JSON retournée
            Map<String, Double> chartData = (Map<String, Double>) result.get("chart_data");

// Vérification de la structure de chart_data
            if (chartData == null || chartData.size() < 4) {
                throw new RuntimeException("Invalid structure in Flask response.");
            }

            String maxCrop = null;
            Double maxValue = Double.NEGATIVE_INFINITY; // Valeur initiale très basse

// Parcourir les données pour trouver le crop avec la valeur maximale
            for (Map.Entry<String, Double> entry : chartData.entrySet()) {
                String crop = entry.getKey();
                Double value = entry.getValue();

                if (value > maxValue) {
                    maxValue = value;
                    maxCrop = crop;
                }
            }

// Extraction des valeurs spécifiques
            String recommendedCrop = maxCrop; // Le crop recommandé
            Double temperature = Double.valueOf(result.get("temperature").toString());
            Double humidity = Double.valueOf(result.get("humidity").toString());
            Double rainfall = Double.valueOf(result.get("rainfall").toString());

            System.out.println("----> recommendedCrop " + recommendedCrop);
            System.out.println("----> temperature " + temperature);
            System.out.println("----> humidity " + humidity);
            System.out.println("----> rainfall " + rainfall);


            // Créer une nouvelle recommandation
            Recommendation recommendation = new Recommendation();
            recommendation.setNitrogen(Double.valueOf(formData.get("nitrogen").toString()));
            recommendation.setPhosphorous(Double.valueOf(formData.get("phosphorous").toString()));
            recommendation.setPottasium(Double.valueOf(formData.get("pottasium").toString()));
            recommendation.setPh(Double.valueOf(formData.get("ph").toString()));
            recommendation.setSeason(formData.get("season").toString());
            recommendation.setCity(formData.get("city").toString());
            recommendation.setTemperature(temperature);
            recommendation.setHumidity(humidity);
            recommendation.setRainfall(rainfall);
            recommendation.setResult(recommendedCrop);
            recommendation.setDate(LocalDateTime.now());

            // Associer l'utilisateur connecté
            User authenticatedUser = userService.findUserProfileByJwt(jwt);
            recommendation.setUser(authenticatedUser);

            // Sauvegarder la recommandation dans la base de données
            Recommendation savedRecommendation = recommendationRepository.save(recommendation);

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("id", savedRecommendation.getId()); // Include the new prediction ID
            finalResponse.put("result", result);

            // Retourner directement la réponse de l'API Flask
            return finalResponse;
        } else {
            throw new RuntimeException("La réponse de l'API Flask est vide ou invalide.");
        }
    }



    public List<Recommendation> getRecommendationsForAuthenticatedUser(User user) {

        return recommendationRepository.findByUser(user);
    }


}






