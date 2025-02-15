package com.example.CropApp.controlleurs;

import com.example.CropApp.entities.Recommendation;
import com.example.CropApp.entities.User;
import com.example.CropApp.services.RecommendationService;
import com.example.CropApp.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {


    @Autowired
    private  RecommendationService recommendationService;

    @Autowired
    private UserService userService;



    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateRecommendation(@RequestBody Map<String, Object> requestData, @RequestHeader("Authorization") String jwt) {
        try {
            // Log pour afficher les données reçues dans la requête
            System.out.println("Received requestData: " + requestData);

            // Appeler le service Flask via RecommendationService
            Map<String, Object> response = recommendationService.generateRecommendation(requestData, jwt);

            // Log pour afficher la réponse reçue du service Flask
            System.out.println("Flask response: " + response);

            // Vérifier si la réponse est nulle ou vide
            if (response == null || response.isEmpty()) {
                throw new RuntimeException("Empty or null response from Flask service");
            }

            // Vérification de la clé "result" dans la réponse
            if (!response.containsKey("result")) {
                throw new RuntimeException("Expected key 'result' not found in response");
            }

            // Extraire et valider le contenu de "result"
            Object resultObject = response.get("result");
            if (resultObject == null) {
                throw new RuntimeException("The value associated with 'result' is either null or not a List");
            }

            // Si tout va bien, retourner la réponse
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // Log de l'exception pour obtenir plus de détails
            e.printStackTrace();

            // Créer un objet de réponse d'erreur avec le message d'exception
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Une erreur s'est produite lors du traitement de la demande.");
            errorResponse.put("message", e.getMessage());

            // Retourner une réponse d'erreur avec les informations pertinentes
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/my-recommendations")
    public ResponseEntity<List<Recommendation>> getMyRecommendations(@RequestHeader("Authorization") String jwt) throws Exception {
        User user = userService.findUserProfileByJwt(jwt);
        try {
            // Récupérer les recommandations de l'utilisateur connecté
            List<Recommendation> recommendations = recommendationService.getRecommendationsForAuthenticatedUser(user);

            // Retourner les recommandations avec un statut HTTP 200
            return ResponseEntity.ok(recommendations);
        } catch (RuntimeException e) {
            // Gérer les erreurs
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }


}