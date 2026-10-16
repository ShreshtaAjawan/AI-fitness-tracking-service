package com.fitness.aiservice.service;

import com.fitness.aiservice.model.Activity;
import com.fitness.aiservice.model.Recommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityAiService {
    private final GeminiService geminiService;

    public Recommendation getRecommendation(Activity activity){
        String prompt = createPromptForActivity(activity);
        String aiResponse = geminiService.getAnswer(prompt);
        log.info("RESPONSE FROM AI :{}",aiResponse);
        return processAIResponse(activity,aiResponse);

    }

    private Recommendation processAIResponse(Activity activity,String aiResponse){
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(aiResponse);

            JsonNode textNode = rootNode.path("steps")
                    .get(1)
                    .path("content")
                    .get(0)
                    .path("text");

            String jsonContent = textNode.asString()
                    .replace("```json\n","")
                    .replace("\n```","")
                    .trim();
            //log.info("PARSED RESPONSE FROM AI :{}",jsonContent);

            JsonNode analysisJson = mapper.readTree(jsonContent);
            JsonNode analysisNode = analysisJson.path("analysis");

            StringBuilder fullAnalysis = new StringBuilder();
            addAnalysisSection(fullAnalysis, analysisNode,"overall", "Overall");
            addAnalysisSection(fullAnalysis, analysisNode,"pace", "Pace");
            addAnalysisSection(fullAnalysis, analysisNode,"heartRate", "HeartRate");
            addAnalysisSection(fullAnalysis, analysisNode,"caloriesBurned", "calories");

            List<String> improvements = extractImprovements(analysisJson.path("improvements"));

            List<String> suggestions = extractSuggestions(analysisJson.path("suggestions"));

            List<String> safety = extractSafetyGuidelines(analysisJson.path("safety"));

            return Recommendation.builder()
                    .activityId(activity.getId())
                    .userId(activity.getUserId())
                    .activityType(activity.getType())
                    .recommendations(fullAnalysis.toString().trim())
                    .improvements(improvements)
                    .suggestions(suggestions)
                    .safety(safety)
                    .createdAt(LocalDateTime.now())
                    .build();


        }catch (Exception e){
            e.printStackTrace();
            return createDefaultRecommendation(activity);
        }
    }

    private Recommendation createDefaultRecommendation(Activity activity) {
        return Recommendation.builder()
                .activityId(activity.getId())
                .userId(activity.getUserId())
                .activityType(activity.getType())
                .recommendations("Unable to generate detailed analysis")
                .improvements(Collections.singletonList("Continue with your current routine"))
                .suggestions(Collections.singletonList("Consider consulting any fitness professional"))
                .safety(Arrays.asList(
                        "Always warm up before exercise",
                        "Stay hydrated",
                        "Listen to your body"
                ))
                .createdAt(LocalDateTime.now())
                .build();

    }

    private List<String> extractSafetyGuidelines(JsonNode safetyNode) {
        List<String> safety = new ArrayList<>();
        if (safetyNode.isArray()) {
            safetyNode.forEach(item -> safety.add(item.asString()));
        }
        return safety.isEmpty() ?
                Collections.singletonList("Follow general safety guidelines") :
                safety;
    }

    private List<String> extractSuggestions(JsonNode suggestionsNode) {
        List<String> suggestions = new ArrayList<>();
        if (suggestionsNode.isArray()) {
            suggestionsNode.forEach(suggestion -> {
                String workout = suggestion.path("workout").asString();
                String description = suggestion.path("description").asString();
                suggestions.add(String.format("%s : %s",workout,description));
            });

        }
        return suggestions.isEmpty() ?
                Collections.singletonList("No specific suggestion provided") :
                suggestions;
    }

    private List<String> extractImprovements(JsonNode improvementsNode) {
        List<String> improvements = new ArrayList<>();
        if(improvementsNode.isArray()){
            improvementsNode.forEach(improvement ->{
                String area = improvement.path("area").asString();
                String detail = improvement.path("recommendation").asString();
                improvements.add(String.format("%s : %s",area,detail));
            });
        }
        return improvements.isEmpty() ?
                Collections.singletonList("No specific improvements provided") :
                improvements;

    }

    private void addAnalysisSection(StringBuilder fullAnalysis, JsonNode analysisNode, String key, String prefix) {
        if(!analysisNode.path(key).isMissingNode()){
            fullAnalysis.append(prefix)
                    .append(analysisNode.path(key).asString())
                    .append("\n\n");
        }
    }

    private String createPromptForActivity(Activity activity) {
        return String.format("""
                Analyze this fitness activity and provide detailed recommendation in the following EXACT JSON format:
                {
                       "analysis": {
                           "overall": "Overall analysis here",
                           "pace": "Pace analysis here",
                           "heartRate": "Heart rate analysis here",
                           "caloBurned": "Calories analysis here"
                       },
                       "improvements": [
                           {
                               "area": "Area name",
                               "recommendation:"Detailed recommendation"
                           }
                       ],
                       "suggestons": [
                           {
                               "workout": "Workout name",
                               "description": "Detailed workout description"
                           }
                       ]
                                     "safety": [
                           "Safety point 1",
                           "Safety ponit 2"
                       ]
                   }
                   
                   Analyze this activity:
                   Activity Type: %s
                   Duration: %d minutes
                   Calories Burned: %d
                   Additional Metrics: %s
                   
                   Provide detailed analysis focusing on performance, improvements,next workout suggestions and safety guidelines.
                   Ensure the response follows the EXACT JSON format shown above. 
                    
                """, activity.getType(),
                activity.getDuration(),
                activity.getCaloriesBurned(),
                activity.getAdditionalMetrics());
    }
}
