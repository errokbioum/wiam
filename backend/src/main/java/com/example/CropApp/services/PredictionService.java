package com.example.CropApp.services;

import com.example.CropApp.entities.Prediction;
import com.example.CropApp.entities.User;
import com.example.CropApp.repositories.PredictionRepository;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@Service
public class PredictionService {


    private final RestTemplate restTemplate;
    @Autowired
    private  UserService userService;
    @Autowired
    private PredictionRepository predictionRepository;
    @Autowired
    public PredictionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Transactional
    public Map<String, Object> callFlaskApi(Map<String, Object> requestData, String jwt) throws Exception {

        String flaskApiUrl = "http://localhost:5000/crop-yield-predict"; // URL de l'API Flask



        // Configurer les en-têtes pour la requête HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Préparer la requête avec les données
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

        // Effectuer l'appel à l'API Flask et récupérer la réponse brute
        ResponseEntity<Map> response = restTemplate.postForEntity(flaskApiUrl, request, Map.class);



        if (response != null && response.getBody() != null) {

            Map<String, Object> responseBody = response.getBody();

            System.out.println("--------->" + response.getBody());

            Map<String, Object> responsee = (Map<String, Object>) responseBody.get("response");

            Map<String, Object> result = (Map<String, Object>) responsee.get("result");

            Map<String, Object> formData = (Map<String, Object>) requestData.get("formdata");
            String city = (String) formData.get("city");
            String crop = (String) formData.get("crop");
            Float area = Float.valueOf(formData.get("area").toString());
            Map<String, Object> resultData = result;

            System.out.println("-----> result " + result);
            Float humidity = Float.valueOf(resultData.get("humidity").toString());
            Float temperature = Float.valueOf(resultData.get("temperature").toString());
            Float rainfall = Float.valueOf(resultData.get("rainfall").toString());
            Float prediction = Float.valueOf(resultData.get("prediction").toString());
            Prediction newPrediction = new Prediction();
            newPrediction.setCity(city);
            newPrediction.setCrop(crop);
            newPrediction.setArea(area);
            newPrediction.setHumidity(humidity);
            newPrediction.setTemperature(temperature);
            newPrediction.setRainfall(rainfall);
            newPrediction.setResult(prediction);
            newPrediction.setDate(LocalDateTime.now());

            // Associer l'utilisateur connecté
            User authenticatedUser = userService.findUserProfileByJwt(jwt);
            newPrediction.setUser(authenticatedUser);

            // Sauvegarder la prédiction dans la base de données
            Prediction savedPrediction = predictionRepository.save(newPrediction);

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("id", savedPrediction.getId()); // Include the new prediction ID
            finalResponse.put("result", result); // Include Flask API result

            return finalResponse;// Retourner directement la réponse de l'API Flask
        } else {
            throw new RuntimeException("La réponse de l'API Flask est vide ou invalide.");
        }
    }




    public List<Prediction> getPredictionsForAuthenticatedUser(User user) {

        return predictionRepository.findByUser(user);
    }


}
