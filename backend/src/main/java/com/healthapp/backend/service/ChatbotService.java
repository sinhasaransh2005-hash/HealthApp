package com.healthapp.backend.service;

import com.healthapp.backend.model.DashboardData;
import com.healthapp.backend.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatbotService {

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    public ChatResponse processQuery(String rawQuery, String clientApiKey, DashboardData dashboardData) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {
            return getWelcomeResponse();
        }

        String resolvedApiKey = (clientApiKey != null && !clientApiKey.trim().isEmpty()) ? clientApiKey.trim() : this.apiKey;

        if (resolvedApiKey != null && !resolvedApiKey.trim().isEmpty()) {
            ChatResponse geminiResponse = callGeminiApi(rawQuery, resolvedApiKey, dashboardData);
            if (geminiResponse != null) {
                return geminiResponse;
            }
        }

        String query = rawQuery.toLowerCase().trim();

        // Conversational Fallbacks (Offline Mode)
        if (query.equals("hello") || query.equals("hi") || query.equals("hey") || query.equals("greetings") || query.contains("good morning") || query.contains("good afternoon") || query.contains("good evening") || query.contains("how are you") || query.contains("how's it going")) {
            String replyText = "Hello! I am **Aria**, your virtual healthcare assistant. 😊\n\n" +
                "I'm doing great, thank you for asking! I'm here to help you stay healthy. You can ask me about symptoms, look at your stats, book a doctor, or just chat!\n\n" +
                "_💡 Tip: To chat about casual general topics, click the ⚙️ icon in the header and save your Gemini API Key to enable my live AI mode!_";
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Check My Stats");
            suggestions.add("How to Book a Doctor");
            suggestions.add("How to use HealthApp");
            return new ChatResponse(replyText, suggestions);
        }

        if (query.contains("who are you") || query.contains("your name") || query.contains("what are you") || query.contains("about you")) {
            String replyText = "I am **Aria**, your personalized virtual healthcare companion. 🌟\n\n" +
                "I was built to help you track daily metrics, consult doctors, and find wellness advice.\n\n" +
                "_💡 Tip: Under the hood, I can connect to Google's live **Gemini API** for full casual AI conversations. Click the ⚙️ icon in the header to enter your API key!_";
            List<String> suggestions = new ArrayList<>();
            suggestions.add("How to use HealthApp");
            suggestions.add("Check My Stats");
            return new ChatResponse(replyText, suggestions);
        }

        if (query.contains("what you can do") || query.contains("what can i ask") || query.contains("features") || query.contains("help me")) {
            String replyText = "I can do quite a lot to keep you on track! 🚀\n" +
                "• **Log stats directly**: Just tell me 'log 500ml water' or 'add 250ml water'!\n" +
                "• **Symptom checks**: Ask me about mild issues like fever, cold, headache, or stomachache.\n" +
                "• **Consultations**: Ask me to book appointments with specialists like Dr. Marcus Chen.\n" +
                "• **App guide**: Ask me how to navigate the Health, Pharmacy, or Profile tabs.\n" +
                "• **Casual Chat**: If you supply a Gemini API Key via the ⚙️ settings panel, I can talk about *any* topic under the sun, just like ChatGPT!";
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Check My Stats");
            suggestions.add("Log 500ml Water");
            suggestions.add("Book Dr. Marcus Chen");
            return new ChatResponse(replyText, suggestions);
        }

        if (query.contains("same answer") || query.contains("repeating") || query.contains("repetitive") || query.contains("again and again") || query.contains("why r u")) {
            String replyText = "I sincerely apologize! 😔\n\n" +
                "Since I am running in **offline fallback mode**, I rely on structured keyword matching. If your message doesn't match a specific health topic, I fallback to my default message.\n\n" +
                "**How to upgrade me:**\n" +
                "1. Obtain a free **Gemini API Key** from Google AI Studio.\n" +
                "2. Click the ⚙️ settings icon at the top right of this chat window.\n" +
                "3. Paste your key and click **Save Key**.\n\n" +
                "This will immediately upgrade me to a live conversational AI (like ChatGPT), allowing us to chat naturally about anything!";
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Check My Stats");
            suggestions.add("How to use HealthApp");
            return new ChatResponse(replyText, suggestions);
        }

        if (query.contains("personal") || query.contains("problem") || query.contains("counseling") || query.contains("life") || query.contains("love") || query.contains("sad") || query.contains("happy") || query.contains("joke") || query.contains("poem") || query.contains("philosophy")) {
            String replyText = "I hear you, and I'd love to chat more about this! 🌸\n\n" +
                "Currently, my offline system is set up for healthcare companion tasks (medical reports, booking appointments, stats logging).\n\n" +
                "For deep personal questions, advice, creative writing, or general chats, click the ⚙️ settings cog above and enter a **Gemini API Key**. It only takes a minute, and it will enable me to chat freely about personal, philosophical, or general questions!";
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Book Dr. Sophia Martinez");
            suggestions.add("Check My Stats");
            return new ChatResponse(replyText, suggestions);
        }

        // Check for local state-changing actions/navigation first
        if ((query.contains("log") || query.contains("add") || query.contains("drink") || query.contains("plus") || query.contains("water") || query.contains("hydrate")) &&
            (query.contains("ml") || query.contains("liter") || query.contains("litre") || query.contains("l") || query.contains("oz") || query.contains("glass"))) {
            
            double liters = 0.25;
            if (query.contains("500") || query.contains("half a liter") || query.contains("half liter")) {
                liters = 0.5;
            } else if (query.contains("750") || query.contains("three quarter")) {
                liters = 0.75;
            } else if (query.contains("1000") || query.contains("1 l") || query.contains("1l") || query.contains("one liter") || query.contains("one l")) {
                liters = 1.0;
            } else if (query.contains("250") || query.contains("a glass") || query.contains("one glass")) {
                liters = 0.25;
            } else if (query.contains("decrease") || query.contains("minus") || query.contains("remove") || query.contains("subtract")) {
                liters = -0.25;
            }
            
            List<String> suggestions = new ArrayList<>();
            suggestions.add("Check My Stats");
            suggestions.add("How to Book a Doctor");
            suggestions.add("How to use HealthApp");
            
            String actionReply;
            if (liters > 0) {
                actionReply = "✅ I've logged **" + (liters >= 1.0 ? liters + " L" : (int)(liters * 1000) + "ml") + "** of water for you! Keep staying hydrated. 💧";
            } else {
                actionReply = "✅ I've removed **" + (int)(Math.abs(liters) * 1000) + "ml** of water from your daily log. 💧";
            }
            
            return new ChatResponse(actionReply, suggestions, "log_water", String.valueOf(liters));
        }

        // Action detection for appointment booking
        for (String doc : new String[]{"Dr. Sarah Jenkins", "Dr. Marcus Chen", "Dr. Elena Rostova", "Dr. Aaron Patel", "Dr. Sophia Martinez"}) {
            String namePart = doc.toLowerCase().substring(4); // "sarah jenkins", etc.
            if (query.contains(namePart) && (query.contains("book") || query.contains("appointment") || query.contains("schedule") || query.contains("consult") || query.contains("reserve"))) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("Check My Stats");
                suggestions.add("Go to Pharmacy");
                
                return new ChatResponse(
                    "🗓️ **Booking Appointment with " + doc + "**\n\n" +
                    "I am initiating a booking process for you. Please choose your desired date and time slot in the booking drawer that has opened!",
                    suggestions,
                    "book_appointment",
                    doc
                );
            }
        }

        // Action detection for navigation
        if (query.contains("navigate") || query.contains("go to") || query.contains("show me") || query.contains("open") || query.contains("switch to")) {
            String targetPage = null;
            String pageName = "";
            if (query.contains("appointment") || query.contains("book") || query.contains("doctor")) {
                targetPage = "appointments";
                pageName = "Appointments";
            } else if (query.contains("health") || query.contains("water") || query.contains("sleep")) {
                targetPage = "health";
                pageName = "Health & Water Log";
            } else if (query.contains("pharmacy") || query.contains("medicine") || query.contains("pill") || query.contains("shop")) {
                targetPage = "pharmacy";
                pageName = "Pharmacy";
            } else if (query.contains("profile") || query.contains("contact") || query.contains("card")) {
                targetPage = "profile";
                pageName = "Profile";
            } else if (query.contains("report") || query.contains("upload") || query.contains("record")) {
                targetPage = "reports";
                pageName = "Reports";
            } else if (query.contains("emergency") || query.contains("sos") || query.contains("ambulance")) {
                targetPage = "emergency";
                pageName = "Emergency";
            } else if (query.contains("home") || query.contains("dashboard")) {
                targetPage = "home";
                pageName = "Home Dashboard";
            }

            if (targetPage != null) {
                List<String> suggestions = new ArrayList<>();
                suggestions.add("Check My Stats");
                suggestions.add("How to use HealthApp");
                return new ChatResponse(
                    "🚀 Navigating to the **" + pageName + "** section...",
                    suggestions,
                    "navigate",
                    targetPage
                );
            }
        }

        // 1. Check for stats/dashboard query
        if (query.contains("stat") || query.contains("score") || query.contains("step") || 
            query.contains("sleep") || query.contains("heart") || query.contains("water") || 
            query.contains("hydration") || query.contains("bpm")) {
            return getStatsResponse(dashboardData);
        }

        // 2. Common Diseases & Medicine suggestions
        if (query.contains("fever") || query.contains("temp") || query.contains("temperature") || query.contains("body hot") || query.contains("feverish")) {
            return getFeverResponse();
        }
        if (query.contains("cough") || query.contains("cuff") || query.contains("sore throat") || query.contains("throat") || query.contains("coughing")) {
            return getCoughResponse();
        }
        if (query.contains("cold") || query.contains("flu") || query.contains("runny nose") || query.contains("blocked nose") || 
            query.contains("sneez") || query.contains("congestion") || query.contains("head cold")) {
            return getColdResponse();
        }
        if (query.contains("headache") || query.contains("head pain") || query.contains("migraine") || query.contains("temple pain")) {
            return getHeadacheResponse();
        }
        if (query.contains("stomach") || query.contains("acid") || query.contains("acidity") || query.contains("indigestion") || 
            query.contains("cramp") || query.contains("constipation") || query.contains("loose motion") || query.contains("diarrhea")) {
            return getStomachResponse();
        }

        // 3. HealthApp Guide / Navigation queries
        if (query.contains("book") || query.contains("appointment") || query.contains("schedule doctor") || query.contains("consult")) {
            return getBookDoctorResponse();
        }
        if (query.contains("report") || query.contains("upload") || query.contains("medical record") || query.contains("pdf") || query.contains("blood test")) {
            return getReportResponse();
        }
        if (query.contains("how to use") || query.contains("guide") || query.contains("navigate") || query.contains("help") || 
            query.contains("tutorial") || query.contains("app feature") || query.contains("what can i do")) {
            return getAppGuideResponse();
        }
        if (query.contains("doctor list") || query.contains("available doctor") || query.contains("who are the doctor") || query.contains("specialist")) {
            return getDoctorListResponse();
        }

        // 4. Greetings
        if (query.contains("hi") || query.contains("hello") || query.contains("hey") || query.contains("greetings") || query.contains("good morning") || query.contains("good afternoon") || query.contains("good evening")) {
            return getWelcomeResponse();
        }

        // 5. Fallback
        return getFallbackResponse();
    }

    private ChatResponse getWelcomeResponse() {
        String reply = "Hello! I am **Aria**, your virtual healthcare assistant. 🌟\n\n" +
                "I can help you with:\n" +
                "• **Medicine suggestions** for mild symptoms like fever, cough, cold, headache, or stomachache.\n" +
                "• **HealthApp navigation** (how to book a doctor, log water, view your stats, upload reports).\n" +
                "• **Dynamic status updates** directly from your dashboard.\n\n" +
                "How can I help you today?";
        
        List<String> suggestions = new ArrayList<>();
        suggestions.add("Medicine for Fever");
        suggestions.add("How to Book a Doctor");
        suggestions.add("Check My Stats");
        suggestions.add("How to use HealthApp");
        
        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getFeverResponse() {
        String reply = "💊 **Medicine Suggestion for Fever:**\n" +
                "• For mild-to-moderate fever and body aches, **Paracetamol (500mg or 650mg)** is the standard choice. It can be taken every 4-6 hours (maximum 4g or 4000mg per day).\n" +
                "• Alternatively, **Ibuprofen (400mg)** can be taken after meals to reduce temperature and inflammation.\n\n" +
                "⚠️ **Precautionary Tips:**\n" +
                "• Rest well and drink plenty of water or electrolyte solutions to prevent dehydration.\n" +
                "• Apply a damp, cool compress to your forehead if needed.\n\n" +
                "🛑 **Medical Disclaimer:**\n" +
                "_If your temperature is over 102°F (38.9°C), persists for more than 3 days, or is accompanied by chest pain, difficulty breathing, or severe headaches, please book an appointment with Dr. Marcus Chen (General Physician) immediately._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Book Dr. Marcus Chen");
        suggestions.add("Medicine for Cough");
        suggestions.add("Check My Stats");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getCoughResponse() {
        String reply = "🥤 **Medicine Suggestion for Cough & Sore Throat:**\n" +
                "• **For Dry Cough** (tickly, no mucus): A cough suppressant syrup containing **Dextromethorphan** helps calm the reflex.\n" +
                "• **For Wet/Chest Cough** (producing mucus): An expectorant syrup containing **Guaifenesin** or **Ambroxol** helps thin and clear the mucus.\n" +
                "• **For Sore Throat**: Lozenges (e.g., Strepsils) or warm saline water gargles 3-4 times a day.\n\n" +
                "⚠️ **Precautionary Tips:**\n" +
                "• Hydrate with warm liquids (herbal teas, warm water with honey and lemon).\n" +
                "• Avoid cold drinks and oily food.\n\n" +
                "🛑 **Medical Disclaimer:**\n" +
                "_If you experience severe wheezing, coughing up blood, high fever, or if the cough lasts longer than 7-10 days, please book an appointment for clinical diagnosis._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Medicine for Cold");
        suggestions.add("Book Dr. Marcus Chen");
        suggestions.add("Check My Stats");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getColdResponse() {
        String reply = "🤧 **Medicine Suggestion for Cold & Congestion:**\n" +
                "• **For Runny Nose & Sneezing**: Non-drowsy antihistamines like **Cetirizine (10mg)** or **Loratadine (10mg)** once daily.\n" +
                "• **For Stuffy/Blocked Nose**: A nasal decongestant spray containing **Oxymetazoline** (use max 3 consecutive days to avoid rebound congestion) or oral **Phenylephrine**.\n" +
                "• **Steam Inhalation**: Add a drop of eucalyptus oil to hot water and inhale steam for 5-10 minutes.\n\n" +
                "⚠️ **Precautionary Tips:**\n" +
                "• Sleep with your head elevated.\n" +
                "• Stay warm and consume hot soups to clear your respiratory passages.\n\n" +
                "🛑 **Medical Disclaimer:**\n" +
                "_If symptoms persist beyond 7-10 days, or if you develop breathing issues or ear pain, seek a professional checkup._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Medicine for Fever");
        suggestions.add("Book Dr. Marcus Chen");
        suggestions.add("Check My Stats");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getHeadacheResponse() {
        String reply = "🧠 **Medicine Suggestion for Headache:**\n" +
                "• For tension or stress headaches, standard pain relievers like **Ibuprofen (400mg)** or **Paracetamol (500mg/650mg)** are highly effective.\n" +
                "• For migraines, **Naproxen Sodium (220mg)** or a combination of paracetamol, aspirin, and caffeine can help.\n\n" +
                "⚠️ **Precautionary Tips:**\n" +
                "• Rest in a quiet, dark room. Apply a cool compress to your forehead or temples.\n" +
                "• Dehydration is a major cause of headaches, so drink a full glass of water immediately.\n\n" +
                "🛑 **Medical Disclaimer:**\n" +
                "_Seek emergency medical care if the headache is sudden, severe (described as the 'worst headache of your life'), or is accompanied by fever, stiff neck, confusion, double vision, or weakness._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Book Dr. Sophia Martinez");
        suggestions.add("Check My Stats");
        suggestions.add("How to log water?");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getStomachResponse() {
        String reply = "🤢 **Medicine Suggestion for Stomach Issues:**\n" +
                "• **For Acidity & Heartburn**: Antacids containing **Magnesium hydroxide/Aluminum hydroxide** (e.g., Gelusil) or H2 blockers like **Famotidine (20mg)**.\n" +
                "• **For Stomach Cramps/Spasms**: Antispasmodic tablets containing **Dicyclomine** help relax intestinal muscles.\n" +
                "• **For Loose Motion/Diarrhea**: Drink plenty of **ORS (Oral Rehydration Salts)** water to replace lost electrolytes. Probiotic supplements can restore gut health.\n\n" +
                "⚠️ **Precautionary Tips:**\n" +
                "• Eat light, bland food (banana, rice, applesauce, toast).\n" +
                "• Avoid dairy, caffeine, and spicy/oily foods.\n\n" +
                "🛑 **Medical Disclaimer:**\n" +
                "_If you have high fever, bloody stools, persistent vomiting, or severe localized abdominal pain, consult a doctor immediately._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Book Dr. Marcus Chen");
        suggestions.add("How to log water?");
        suggestions.add("Check My Stats");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getBookDoctorResponse() {
        String reply = "🗓️ **How to Book a Doctor's Appointment:**\n" +
                "1. Go to the **Appointments** tab using the bottom navigation bar.\n" +
                "2. Browse the available specialist doctors (Cardiologist, Physician, Dermatologist, Pediatrician, Neurologist).\n" +
                "3. Click the **Book Now** button on the chosen doctor's card.\n" +
                "4. Select a booking date and your preferred time slot.\n" +
                "5. Tap **Confirm Booking** to save your slot.\n\n" +
                "ℹ️ _Note: Each doctor accepts a maximum of 15 bookings per day to ensure dedicated patient care._";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("List available doctors");
        suggestions.add("Check My Stats");
        suggestions.add("How to use HealthApp");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getReportResponse() {
        String reply = "📄 **How to Upload Medical Reports:**\n" +
                "1. From the Home dashboard, find the **Quick Actions** panel.\n" +
                "2. Click on the **Reports** button.\n" +
                "3. Tap inside the dashed **Upload Zone** box.\n" +
                "4. Select an image file (JPEG, PNG, GIF) of your medical report.\n" +
                "5. The file will upload securely, and you'll be able to view it anytime!";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("How to use HealthApp");
        suggestions.add("Check My Stats");
        suggestions.add("List available doctors");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getDoctorListResponse() {
        String reply = "👩‍⚕️ **HealthTrack Specialists Available:**\n\n" +
                "1. **Dr. Sarah Jenkins** (Cardiologist)\n" +
                "   • _Specialty:_ Heart care, blood pressure, and cardiovascular wellness.\n" +
                "2. **Dr. Marcus Chen** (General Physician)\n" +
                "   • _Specialty:_ General illness (fever, cold), chronic care, family medicine.\n" +
                "3. **Dr. Elena Rostova** (Dermatologist)\n" +
                "   • _Specialty:_ Skin health, hair, nails, acne, and allergy rash.\n" +
                "4. **Dr. Aaron Patel** (Pediatrician)\n" +
                "   • _Specialty:_ Infant, child, and teenage growth tracking and immunization.\n" +
                "5. **Dr. Sophia Martinez** (Neurologist)\n" +
                "   • _Specialty:_ Migraines, sleep disorders, nerves, and brain function.\n\n" +
                "👉 _You can book consultations with any of these doctors in the App!_";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("How to Book a Doctor");
        suggestions.add("Book Dr. Marcus Chen");
        suggestions.add("Check My Stats");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getAppGuideResponse() {
        String reply = "🗺️ **HealthApp Navigation Guide:**\n" +
                "• **Home**: Check your health score, see scheduled appointments, and click the **sliders icon** at the top right to manually update your metrics.\n" +
                "• **Appointments**: Find a doctor and book slot-validated consultations.\n" +
                "• **Health**: Log your daily water intake (click +250ml / +500ml), track sleep goals, and read daily healthy tips.\n" +
                "• **Pharmacy**: Maintain your daily pill check-list and shop for health/medicine items.\n" +
                "• **Profile**: Save emergency contacts and manage personal health cards.";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Check My Stats");
        suggestions.add("How to log water?");
        suggestions.add("Medicine for Fever");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getStatsResponse(DashboardData dashboardData) {
        if (dashboardData == null) {
            return new ChatResponse("I currently cannot fetch your dashboard data. Please try again later.", new ArrayList<>());
        }

        int score = dashboardData.getHealthScore();
        String scoreEmoji = score >= 80 ? "🔥 (Excellent)" : "⚡ (Needs Improvement)";

        String reply = "📊 **Your Current Health Track Stats:**\n" +
                "• **Health Score:** " + score + "/100 " + scoreEmoji + "\n" +
                "• **Heart Rate:** " + dashboardData.getHeartRate() + " BPM\n" +
                "• **Daily Steps:** " + String.format("%,d", dashboardData.getSteps()) + " steps\n" +
                "• **Water Logged:** " + String.format("%.1f", dashboardData.getWater()) + " Liters\n" +
                "• **Sleep Duration:** " + String.format("%.1f", dashboardData.getSleep()) + " Hours\n\n" +
                "🗓️ **Upcoming Appointment:**\n" +
                (dashboardData.getAppointmentDoctor() != null && !dashboardData.getAppointmentDoctor().equalsIgnoreCase("No upcoming appointments") ?
                        "• **" + dashboardData.getAppointmentDoctor() + "** on " + dashboardData.getAppointmentTime() :
                        "• No appointments booked currently. Need to see someone?") + "\n\n" +
                "💡 _Tip: You can update these stats manually by clicking the sliders icon in the dashboard header!_";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("How to Book a Doctor");
        if (dashboardData.getWater() < 2.5) {
            suggestions.add("How to log water?");
        }
        suggestions.add("How to use HealthApp");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse getFallbackResponse() {
        String reply = "🤔 I didn't quite catch that. I can help with:\n" +
                "• OTC medicine suggestions for common issues like **fever, cough, cold, headache, or stomachache**.\n" +
                "• Guides on **booking appointments, logging water, or uploading reports**.\n" +
                "• Fetching your **health stats**.\n\n" +
                "Please type a query or select one of the quick options below:";

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Medicine for Fever");
        suggestions.add("How to Book a Doctor");
        suggestions.add("Check My Stats");
        suggestions.add("How to use HealthApp");

        return new ChatResponse(reply, suggestions);
    }

    private ChatResponse callGeminiApi(String rawQuery, String targetApiKey, DashboardData dashboardData) {
        if (targetApiKey == null || targetApiKey.trim().isEmpty()) {
            return null;
        }

        try {
            // Build the prompt containing system instructions and current state
            String systemInstructions = 
                "You are Aria, a premium virtual healthcare assistant on the HealthTrack app.\n" +
                "You help users with suggestions for mild symptoms, navigating the app, and viewing dashboard stats.\n" +
                "Here is the user's current live state:\n" +
                "- Health Score: " + dashboardData.getHealthScore() + "/100\n" +
                "- Heart Rate: " + dashboardData.getHeartRate() + " BPM\n" +
                "- Steps: " + dashboardData.getSteps() + " steps\n" +
                "- Water Logged: " + dashboardData.getWater() + " Liters\n" +
                "- Sleep: " + dashboardData.getSleep() + " Hours\n" +
                "- Upcoming Appointment: " + (dashboardData.getAppointmentDoctor() != null && !dashboardData.getAppointmentDoctor().isEmpty() ? dashboardData.getAppointmentDoctor() + " at " + dashboardData.getAppointmentTime() : "None") + "\n\n" +
                "Available Specialist Doctors in HealthTrack:\n" +
                "1. Dr. Sarah Jenkins (Cardiologist) - Cardiology\n" +
                "2. Dr. Marcus Chen (General Physician) - General symptoms (fever, cough, cold, etc.)\n" +
                "3. Dr. Elena Rostova (Dermatologist) - Skin, hair, nails\n" +
                "4. Dr. Aaron Patel (Pediatrician) - Child care\n" +
                "5. Dr. Sophia Martinez (Neurologist) - Brain, migraines, sleep\n\n" +
                "Special Action instructions:\n" +
                "If the user asks you to log water (e.g. 'log 250ml water' or 'add 500ml water'), your response MUST contain a special command format at the very end of your response, starting on a new line, like: ACTION:log_water:0.25 (or ACTION:log_water:-0.25 if subtracting). Log values in Liters.\n" +
                "If the user wants to book a doctor (e.g. 'book Dr. Marcus Chen'), your response MUST contain a command at the end, starting on a new line, like: ACTION:book_appointment:Doctor Name. E.g. ACTION:book_appointment:Dr. Marcus Chen.\n" +
                "If the user wants to go to/navigate to a page, write: ACTION:navigate:page_name (where page_name is one of: home, appointments, health, pharmacy, profile, reports, emergency).\n\n" +
                "Format your main response text using clean Markdown (e.g., bullet lists and bold text). Keep responses concise, friendly, and helpful. Always add a medical disclaimer for symptom queries.\n" +
                "Suggestions list: Please output up to 3 short suggestion chips for the user at the end of the text in the format: SUGGESTION:Suggestion Text (one suggestion per line).";

            // Prepare HTTP Request Body
            String requestJson = 
                "{" +
                    "\"contents\": [{" +
                        "\"parts\": [" +
                            "{\"text\": \"" + systemInstructions.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\\n\\nUser Message: " + rawQuery.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"}" +
                        "]" +
                    "}]," +
                    "\"generationConfig\": {" +
                        "\"maxOutputTokens\": 500" +
                    "}" +
                "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + targetApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String fullText = extractTextFromJson(response.body());
                if (fullText != null) {
                    return parseGeminiText(fullText);
                }
            } else {
                System.err.println("Gemini API returned error code: " + response.statusCode() + " Body: " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private String extractTextFromJson(String json) {
        if (json == null) return null;
        int textIndex = json.indexOf("\"text\": \"");
        if (textIndex == -1) return null;
        
        int startIndex = textIndex + 9;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') sb.append('\n');
                else if (c == 't') sb.append('\t');
                else if (c == 'r') sb.append('\r');
                else if (c == 'b') sb.append('\b');
                else if (c == 'f') sb.append('\f');
                else sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // end of string
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private ChatResponse parseGeminiText(String fullText) {
        String reply = fullText;
        String action = null;
        String actionData = null;
        List<String> suggestions = new ArrayList<>();

        String[] lines = fullText.split("\n");
        StringBuilder replyBuilder = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("ACTION:")) {
                String[] parts = trimmed.substring(7).split(":", 2);
                if (parts.length > 0) {
                    action = parts[0].trim();
                }
                if (parts.length > 1) {
                    actionData = parts[1].trim();
                }
            } else if (trimmed.startsWith("SUGGESTION:")) {
                suggestions.add(trimmed.substring(11).trim());
            } else {
                replyBuilder.append(line).append("\n");
            }
        }

        reply = replyBuilder.toString().trim();
        
        if (suggestions.isEmpty()) {
            suggestions.add("Medicine for Fever");
            suggestions.add("How to Book a Doctor");
            suggestions.add("Check My Stats");
        }

        return new ChatResponse(reply, suggestions, action, actionData);
    }
}
