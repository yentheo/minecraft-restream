package one.spectra.minecraft.restream.restream;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import one.spectra.minecraft.restream.OnChatMessageHandler;
import one.spectra.minecraft.restream.OnConnectHandler;
import one.spectra.minecraft.restream.OnDisconnectHandler;
import one.spectra.minecraft.restream.OnMessageHandler;
import one.spectra.minecraft.restream.Platform;
import one.spectra.minecraft.restream.SimpleClient;
import one.spectra.minecraft.restream.configuration.ConfigurationManager;
import one.spectra.minecraft.restream.configuration.RestreamConfiguration;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.stream.Collectors;

class Color {
    int r;
    int g;
    int b;
    Formatting formatting;

    Color(int r, int g, int b, Formatting formatting) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.formatting = formatting;
    }
}

public class RestreamClient {
    private ConfigurationManager _configurationManager;

    private List<Color> _minecraftColors = new ArrayList<Color>();

    public RestreamClient(ConfigurationManager configurationManager) {
        _minecraftColors.add(new Color(170, 0, 0, Formatting.DARK_RED));
        _minecraftColors.add(new Color(255, 85, 85, Formatting.RED));
        _minecraftColors.add(new Color(255, 170, 0, Formatting.GOLD));
        _minecraftColors.add(new Color(255, 255, 85, Formatting.YELLOW));
        _minecraftColors.add(new Color(0, 170, 0, Formatting.DARK_GREEN));
        _minecraftColors.add(new Color(85, 255, 85, Formatting.GREEN));
        _minecraftColors.add(new Color(85, 255, 255, Formatting.AQUA));
        _minecraftColors.add(new Color(0, 170, 170, Formatting.DARK_AQUA));
        _minecraftColors.add(new Color(0, 0, 170, Formatting.DARK_BLUE));
        _minecraftColors.add(new Color(85, 85, 255, Formatting.BLUE));
        _minecraftColors.add(new Color(255, 85, 255, Formatting.LIGHT_PURPLE));
        _minecraftColors.add(new Color(170, 0, 170, Formatting.DARK_PURPLE));
        _minecraftColors.add(new Color(255, 255, 255, Formatting.WHITE));
        _minecraftColors.add(new Color(170, 170, 170, Formatting.GRAY));
        _minecraftColors.add(new Color(85, 85, 85, Formatting.DARK_GRAY));
        _minecraftColors.add(new Color(0, 0, 0, Formatting.BLACK));
        _configurationManager = configurationManager;
    }

    public AuthorizeResponse authorize(String code) {
        System.out.println("Authorizing with code");
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("grant_type", "authorization_code");
        parameters.put("redirect_uri", "http://localhost");
        parameters.put("code", code);
        return authorize(parameters);
    }

    public AuthorizeResponse refreshAuthorizationFor(String name) {
        System.out.println("Authorizing with refresh token");
        String refreshToken = _configurationManager.getRefreshToken(name);
        if (refreshToken != null) {
            HashMap<String, String> parameters = new HashMap<String, String>();
            parameters.put("grant_type", "refresh_token");
            parameters.put("refresh_token", refreshToken);
            return authorize(parameters);
        }
        return null;
    }

    public void startListen(String accessToken, OnConnectHandler onConnectHandler, OnChatMessageHandler handler,
            OnDisconnectHandler onDisconnectHandler) {
        SimpleClient client;
        try {
            client = new SimpleClient(new URI("wss://chat.api.restream.io/ws?accessToken=" + accessToken));
            client.registerOnOpen(() -> {
                onConnectHandler.op();
            });

            client.registerOnMessage(message -> {
                System.out.println(message);
                Gson gson = new Gson();
                if (message != null) {
                    JsonObject json = gson.fromJson(message, JsonObject.class);
                    String action = json.get("action").getAsString();
                    if ("event".equals(action)) {
                        JsonObject payloadJson = json.get("payload").getAsJsonObject();
                        JsonObject eventPayloadJson = payloadJson.get("eventPayload").getAsJsonObject();
                        int eventSourceId = payloadJson.get("eventSourceId").getAsInt();
                        if (eventSourceId == 2) { // twitch
                            JsonObject authorJson = eventPayloadJson.get("author").getAsJsonObject();
                            String displayName = authorJson.get("displayName").getAsString();
                            int color = 16777215;
                            if (!authorJson.get("color").isJsonNull()) {
                                color = hexColorToInt(authorJson.get("color").getAsString());
                            };
                            String text = eventPayloadJson.get("text").getAsString();
                            LiteralText authorText = new LiteralText(displayName);
                            Style authorStyle = authorText.getStyle().withColor(TextColor.fromRgb(color));
                            authorText.setStyle(authorStyle);
                            Text messageText = new LiteralText(text).formatted(Formatting.WHITE);
                            handler.op(authorText, messageText, Platform.TWITCH);
                        } else if (eventSourceId == 13) { // youtube

                        }
                    }
                }
            });

            client.registerOnDisconnect(() -> {
                onDisconnectHandler.op();
            });

            client.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private int hexColorToInt(String hexColor) {
        return Integer.parseInt(hexColor.substring(1, 7), 16);
    }

    private Formatting getFormatting(String hexColor) {
        if (hexColor == null) {
            return Formatting.WHITE;
        }

        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        double lowestValue = Double.MAX_VALUE;
        Formatting result = Formatting.WHITE;
        for (Color color : _minecraftColors) {
            double d = Math.pow(((color.r - r) * 0.30), 2) + Math.pow(((color.g - g) * 0.59), 2)
                    + Math.pow(((color.b - b) * 0.11), 2);
            if (d < lowestValue) {
                lowestValue = d;
                result = color.formatting;
            }
        }

        return result;
    }

    private AuthorizeResponse authorize(HashMap<String, String> parameters) {
        RestreamConfiguration configuration = _configurationManager.getConfiguration();
        HashMap<String, String> allParameters = new HashMap<String, String>(parameters);
        allParameters.put("client_id", configuration.clientId);
        allParameters.put("client_secret", configuration.clientSecret);
        System.out.println("Authorizing with client id {" + configuration.clientId + "} and secret {"
                + configuration.clientSecret + "}");
        List<String> keyValues = allParameters.keySet().stream()
                .map(key -> String.format("%s=%s", key, allParameters.get(key))).collect(Collectors.toList());
        String bodyContent = String.join("&", keyValues);
        MediaType formUrl = MediaType.get("application/x-www-form-urlencoded");
        System.out.println("Authorizing with body {" + bodyContent + "}");
        RequestBody body = RequestBody.create(bodyContent, formUrl);
        Request request = new Request.Builder().url("https://api.restream.io/oauth/token").post(body).build();
        OkHttpClient httpClient = new OkHttpClient();
        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            System.out.println(responseBody);
            Gson gson = new Gson();
            AuthorizeResponse authresponse = (AuthorizeResponse) gson.fromJson(responseBody, AuthorizeResponse.class);
            return authresponse;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}