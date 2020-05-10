package one.spectra.minecraft.restream;

import java.util.HashMap;
import java.util.Random;
import java.util.stream.IntStream;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent.Action;
import net.minecraft.util.Formatting;

public class LinkTextGenerator {

    public static String generateLink() {
        HashMap<String, String> queryStringParameters = new HashMap<String, String>();
        queryStringParameters.put("response_type", "code");
        queryStringParameters.put("client_id", "033a362e-10fe-4a6d-8c6f-3f55f99584d3");
        queryStringParameters.put("redirect_uri", "http://localhost");
        queryStringParameters.put("state", generateRandomString());
        String baseUrl = "https://api.restream.io/login";
        String queryString = queryStringParameters.entrySet().stream().map(x -> x.getKey() + '=' + x.getValue())
                .reduce((previous, current) -> previous + "&" + current).orElse("");
        return baseUrl + "?" + queryString;
    }

    public static LiteralText generateLinkText(String text, String url) {
        LiteralText linkText = new LiteralText("Restream");
        ClickEvent clickEvent = new ClickEvent(Action.OPEN_URL, url);
        Style linkStyle = linkText.getStyle().withFormatting(Formatting.UNDERLINE).withClickEvent(clickEvent);
        linkText.setStyle(linkStyle);
        return linkText;
    }

    private static String generateRandomString() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        return IntStream.rangeClosed(1, 8).mapToObj(i -> chars)
                .map(c -> c.charAt(new Random().nextInt(chars.length() - 1))).map(c -> c.toString())
                .reduce("", (previous, current) -> previous + current);
    }
}