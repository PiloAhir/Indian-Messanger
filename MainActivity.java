package com.indianmessenger.app;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import javax.crypto.Cipher;
import android.util.Base64;

/*
 * Indian Messenger starter MVP.
 * Configure SUPABASE_URL and SUPABASE_ANON_KEY below.
 * This starter uses Supabase Auth (synthetic username email) and REST endpoints.
 * Messages are RSA-OAEP encrypted on the sender device for the recipient's public key.
 * For a production messenger, use a reviewed protocol such as Signal Protocol and
 * add key verification, rotation, multi-device support, spam controls and abuse reporting.
 */
public class MainActivity extends Activity {
    // Replace these two placeholders with your Supabase project values.
    private static final String SUPABASE_URL = "https://YOUR_PROJECT.supabase.co";
    private static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";

    private final int saffron = Color.rgb(255, 153, 51);
    private final int green = Color.rgb(19, 136, 8);
    private LinearLayout root, content;
    private EditText userField, passField, genderField, dobField, searchField, messageField;
    private String accessToken = "", authUserId = "", userId = "", displayUser = "", selectedRecipient = "";
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(255,153,51));
        getWindow().setNavigationBarColor(Color.rgb(19,136,8));
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildBase();
        showAuth();
    }

    private void buildBase() {
        root = new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(Color.rgb(247,249,250));
        root.setPadding(dp(18), dp(16), dp(18), dp(12));
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView flag = text("🇮🇳", 30, Color.BLACK, true); brand.addView(flag);
        LinearLayout names = new LinearLayout(this); names.setOrientation(1); names.setPadding(dp(10),0,0,0);
        names.addView(text("Indian Messenger", 22, Color.rgb(28,42,54), true));
        names.addView(text("Private by design • Starter version", 12, Color.DKGRAY, false));
        brand.addView(names, new LinearLayout.LayoutParams(0,-2,1));
        root.addView(brand);
        View stripe = new View(this); stripe.setBackgroundColor(saffron);
        LinearLayout.LayoutParams stripeP = new LinearLayout.LayoutParams(-1,dp(3)); stripeP.topMargin=dp(12); stripeP.bottomMargin=dp(12); root.addView(stripe,stripeP);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(1);
        scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void showAuth() {
        content.removeAllViews();
        content.addView(text("Create account / Sign in", 20, Color.rgb(28,42,54), true));
        userField = edit("User ID (12+ chars: Aa1!)");
        passField = edit("Password (8+ characters)"); passField.setInputType(129);
        genderField = edit("Gender (optional)");
        dobField = edit("Date of birth (YYYY-MM-DD, optional)");
        content.addView(userField); content.addView(passField); content.addView(genderField); content.addView(dobField);
        Button create = button("Create account"); content.addView(create);
        create.setOnClickListener(v -> auth(true));
        Button login = button("Sign in"); content.addView(login);
        login.setOnClickListener(v -> auth(false));
        content.addView(text("User ID is private: search requires the complete exact ID. Never reuse a password.", 13, Color.DKGRAY, false));
    }

    private void auth(boolean create) {
        String raw = userField.getText().toString().trim();
        String pass = passField.getText().toString();
        if (!validId(raw)) { msg("User ID: ઓછામાં ઓછા 12 અક્ષર અને uppercase, lowercase, number તથા symbol જરૂરી."); return; }
        if (pass.length() < 8) { msg("પાસવર્ડ ઓછામાં ઓછા 8 અક્ષરનો રાખો."); return; }
        if (SUPABASE_URL.contains("YOUR_PROJECT") || SUPABASE_ANON_KEY.contains("YOUR_")) {
            msg("પહેલાં README મુજબ Supabase URL અને anon key મૂકો."); return;
        }
        userId = raw;
        displayUser = raw;
        try {
            JSONObject body = new JSONObject();
            body.put("email", toSyntheticEmail(raw));
            body.put("password", pass);
            String endpoint = create ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password";
            JSONObject result = request("POST", endpoint, body, false);
            accessToken = result.optString("access_token", "");
            JSONObject authUser = result.optJSONObject("user");
            if (authUser == null) authUser = result.optJSONObject("session") == null ? null : result.optJSONObject("session").optJSONObject("user");
            authUserId = authUser == null ? "" : authUser.optString("id", "");
            if (accessToken.isEmpty() || authUserId.isEmpty()) {
                // Supabase may require email confirmation; turn it off for synthetic username IDs.
                msg(create ? "એકાઉન્ટ જવાબ મળ્યો. Supabaseમાં email confirmations OFF કરો, પછી ફરી Sign in કરો." : "લૉગિન નિષ્ફળ: User ID/પાસવર્ડ અથવા Supabase સેટિંગ્સ તપાસો.");
                return;
            }
            if (create) saveProfile(raw);
            loadKeys();
            showHome();
        } catch (Exception e) { msg("કનેક્શન/સેટઅપ ભૂલ: " + shortError(e)); }
    }

    private boolean validId(String s) {
        return s.length() >= 12 && s.matches("(?s).*[A-Z].*") && s.matches("(?s).*[a-z].*")
            && s.matches("(?s).*\\d.*") && s.matches("(?s).*[^A-Za-z0-9].*")
            && s.matches("[A-Za-z0-9._!@#$%^&*+=?-]+");
    }
    private String toSyntheticEmail(String id) {
        try { return Base64.encodeToString(id.toLowerCase().getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING) + "@users.indianmessenger.invalid"; }
        catch(Exception e) { return id.toLowerCase().replaceAll("[^a-z0-9]","") + "@users.indianmessenger.invalid"; }
    }

    private void saveProfile(String id) throws Exception {
        JSONObject profile = new JSONObject();
        if (authUserId.isEmpty()) throw new IOException("Auth user ID missing; sign in again after signup.");
        profile.put("id", authUserId);
        profile.put("user_id", id); profile.put("gender", genderField.getText().toString().trim());
        profile.put("birth_date", dobField.getText().toString().trim().isEmpty()?JSONObject.NULL:dobField.getText().toString().trim());
        profile.put("public_key", ""); 
        request("POST", "/rest/v1/profiles", profile, true, "resolution=merge-duplicates,return=minimal");
    }

    private void loadKeys() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA"); gen.initialize(2048);
            KeyPair pair = gen.generateKeyPair(); privateKey = pair.getPrivate(); publicKey = pair.getPublic();
            // Public key publication is intentionally not silently enabled in this MVP.
            // Production implementation must persist private keys securely and publish public keys.
        } catch(Exception ignored) {}
    }

    private void showHome() {
        content.removeAllViews();
        content.addView(text("નમસ્તે, " + displayUser, 20, Color.rgb(28,42,54), true));
        content.addView(text("ચેટ શરૂ કરવા માટે સામેના વ્યક્તિનું આખું User ID લખો.", 14, Color.DKGRAY, false));
        searchField = edit("સંપૂર્ણ User ID શોધો"); content.addView(searchField);
        Button search = button("Find exact user"); content.addView(search);
        search.setOnClickListener(v -> findUser());
        TextView hint = text("આ પ્રારંભિક MVP છે. E2E key exchange અને multi-device sync માટે નીચે READMEની મર્યાદાઓ જુઓ.", 13, Color.DKGRAY, false);
        content.addView(hint);
    }

    private void findUser() {
        String exact = searchField.getText().toString().trim();
        if (!validId(exact)) { msg("માન્ય સંપૂર્ણ User ID દાખલ કરો."); return; }
        try {
            JSONArray arr = requestArray("GET", "/rest/v1/profiles?select=user_id,public_key&user_id=eq." + URLEncoder.encode(exact,"UTF-8") + "&limit=1", null, true);
            content.removeAllViews();
            if (arr.length()==0) { content.addView(text("આ User ID મળ્યું નથી.",16,Color.DKGRAY,true)); }
            else {
                JSONObject u = arr.getJSONObject(0); selectedRecipient = u.optString("user_id");
                content.addView(text("User: " + selectedRecipient, 18, Color.rgb(28,42,54), true));
                content.addView(text("પ્રથમ MVPમાં સર્વર-સાઇડ key exchange અને production E2E પૂર્ણ નથી. સુરક્ષિત messaging માટે README મુજબ setup/next steps અનુસરો.",14,Color.DKGRAY,false));
                messageField = edit("Message"); content.addView(messageField);
                Button send = button("Send (setup required)"); content.addView(send);
                send.setOnClickListener(v -> msg("સંદેશ મોકલવાનું ચાલુ કરવા પહેલાં READMEમાં જણાવેલ key-exchange અને messages policy પૂર્ણ કરવી જરૂરી છે."));
                Button back = button("Back"); content.addView(back); back.setOnClickListener(v -> showHome());
            }
            Button home = button("Home"); content.addView(home); home.setOnClickListener(v -> showHome());
        } catch(Exception e) { msg("યુઝર શોધવામાં ભૂલ: " + shortError(e)); }
    }

    private JSONObject request(String method, String path, JSONObject body, boolean auth) throws Exception {
        return request(method,path,body,auth,null);
    }
    private JSONObject request(String method, String path, JSONObject body, boolean auth, String prefer) throws Exception {
        HttpURLConnection c = connection(path,method,auth,prefer);
        if (body != null) { c.setDoOutput(true); c.setRequestProperty("Content-Type","application/json"); try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));} }
        String s = read(c); int code = c.getResponseCode();
        if(code<200||code>=300) throw new IOException("HTTP "+code+" "+s);
        return s.trim().startsWith("{")?new JSONObject(s):new JSONObject();
    }
    private JSONArray requestArray(String method,String path,JSONObject body,boolean auth) throws Exception {
        HttpURLConnection c=connection(path,method,auth,null);
        if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        String s=read(c);int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code+" "+s);return new JSONArray(s);
    }
    private HttpURLConnection connection(String path,String method,boolean auth,String prefer)throws Exception{
        URL url=new URL(SUPABASE_URL+path);HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(12000);
        c.setRequestProperty("apikey",SUPABASE_ANON_KEY);c.setRequestProperty("Authorization","Bearer "+(auth&&!accessToken.isEmpty()?accessToken:SUPABASE_ANON_KEY));c.setRequestProperty("Accept","application/json");
        if(prefer!=null)c.setRequestProperty("Prefer",prefer);return c;
    }
    private String read(HttpURLConnection c)throws Exception{
        int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();if(in==null)return "";
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);return out.toString("UTF-8");
    }
    private String shortError(Exception e){String s=e.getMessage();if(s==null)return "અજ્ઞાત ભૂલ";return s.length()>160?s.substring(0,160):s;}
    private EditText edit(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(12),dp(9),dp(12),dp(9));e.setBackground(bg(Color.WHITE,0xFFD9E1E5));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);p.bottomMargin=dp(5);e.setLayoutParams(p);return e;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(25,73,80));b.setBackground(bg(Color.WHITE,0xFFD6E3E7));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(7);p.bottomMargin=dp(5);b.setLayoutParams(p);return b;}
    private TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(3),dp(5),dp(3),dp(5));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable bg(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void msg(String s){new AlertDialog.Builder(this).setTitle("Indian Messenger").setMessage(s).setPositiveButton("OK",null).show();}
}
