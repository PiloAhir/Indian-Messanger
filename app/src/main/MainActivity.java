package com.indianmessenger.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Base64;

public class MainActivity extends Activity {
    private static final String SUPABASE_URL = "https://YOUR_PROJECT.supabase.co";
    private static final String SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY";
    private final int saffron = Color.rgb(255, 153, 51);
    private LinearLayout root, content;
    private EditText userField, passField, genderField, dobField, searchField, messageField;
    private String accessToken = "", authUserId = "", userId = "", displayUser = "", selectedRecipient = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247, 249, 250));
        getWindow().setNavigationBarColor(Color.rgb(247, 249, 250));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        buildBase(); showAuth();
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }

    private void buildBase() {
        root = new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(Color.rgb(247,249,250)); root.setPadding(dp(18),dp(12),dp(18),dp(12));
        LinearLayout brand = new LinearLayout(this); brand.setGravity(Gravity.CENTER_VERTICAL); brand.addView(text("🇮🇳",30,Color.BLACK,true));
        LinearLayout names = new LinearLayout(this); names.setOrientation(1); names.setPadding(dp(10),0,0,0);
        names.addView(text("Indian Messenger",22,Color.rgb(28,42,54),true)); names.addView(text("Private by design • Starter version",12,Color.DKGRAY,false));
        brand.addView(names,new LinearLayout.LayoutParams(0,-2,1)); root.addView(brand);
        View stripe = new View(this); stripe.setBackgroundColor(saffron); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1,dp(3)); sp.topMargin=dp(12); sp.bottomMargin=dp(12); root.addView(stripe,sp);
        ScrollView scroll = new ScrollView(this); content = new LinearLayout(this); content.setOrientation(1); scroll.addView(content); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    private void showAuth() {
        content.removeAllViews(); content.addView(text("Create account / Sign in",22,Color.rgb(28,42,54),true));
        userField=edit("User ID (12+ chars: Aa1!)"); passField=edit("Password (8+ characters)");
        passField.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        genderField=edit("Gender (optional)"); dobField=edit("Date of birth (YYYY-MM-DD, optional)");
        content.addView(userField); content.addView(passField); content.addView(genderField); content.addView(dobField);
        Button create=button("Create account"); content.addView(create); create.setOnClickListener(v->auth(true,create));
        Button login=button("Sign in"); content.addView(login); login.setOnClickListener(v->auth(false,login));
        content.addView(text("User ID ઓછામાં ઓછું 12 અક્ષરનું હોવું જોઈએ. તેમાં uppercase, lowercase, નંબર અને symbol હોવા જોઈએ.",13,Color.DKGRAY,false));
    }
    private boolean validId(String s) {
        return s.length()>=12 && s.matches("(?s).*[A-Z].*") && s.matches("(?s).*[a-z].*") && s.matches("(?s).*\\d.*") && s.matches("(?s).*[^A-Za-z0-9].*") && s.matches("[A-Za-z0-9._!@#$%^&*+=?-]+");
    }
    private boolean configured() { return !SUPABASE_URL.contains("YOUR_PROJECT") && !SUPABASE_ANON_KEY.contains("YOUR_") && SUPABASE_URL.startsWith("https://") && !SUPABASE_ANON_KEY.trim().isEmpty(); }

    private void auth(boolean create, Button clickedButton) {
        final String raw=userField.getText().toString().trim(), pass=passField.getText().toString(), gender=genderField.getText().toString().trim(), dob=dobField.getText().toString().trim();
        if(!validId(raw)){msg("User ID ઓછામાં ઓછા 12 અક્ષરનું હોવું જોઈએ અને તેમાં uppercase, lowercase, નંબર તથા symbol જરૂરી છે.");return;}
        if(pass.length()<8){msg("પાસવર્ડ ઓછામાં ઓછા 8 અક્ષરનો રાખો.");return;}
        if(!configured()){msg("MainActivity.java માં વાસ્તવિક Supabase Project URL અને public anon/publishable key મૂકો.");return;}
        clickedButton.setEnabled(false); clickedButton.setText("કૃપા કરીને રાહ જુઓ...");
        executor.execute(()->{
            String error=null, token="", returnedId=""; boolean success=false, profileSaved=false;
            try {
                JSONObject body=new JSONObject(); body.put("email",toSyntheticEmail(raw)); body.put("password",pass);
                String endpoint=create?"/auth/v1/signup":"/auth/v1/token?grant_type=password";
                JSONObject result=request("POST",endpoint,body,false,null,""); token=result.optString("access_token","");
                JSONObject authUser=result.optJSONObject("user");
                if(authUser==null){JSONObject session=result.optJSONObject("session");if(session!=null){authUser=session.optJSONObject("user");if(token.isEmpty())token=session.optString("access_token","");}}
                if(authUser==null)throw new IOException(create?"એકાઉન્ટનો જવાબ મળ્યો, પરંતુ session મળ્યો નથી. Supabase email confirmation settings તપાસો.":"લૉગિન નિષ્ફળ. User ID, પાસવર્ડ અને Supabase settings તપાસો.");
                returnedId=authUser.optString("id","");
                if(token.isEmpty()||returnedId.isEmpty())throw new IOException("Supabase session મળ્યું નથી. Email confirmation ચાલુ હોય તો તેને બંધ કરીને પરીક્ષણ કરો, અથવા ઇમેઇલ કન્ફર્મ કર્યા પછી Sign in કરો.");
                if(create){JSONObject profile=new JSONObject();profile.put("id",returnedId);profile.put("user_id",raw);profile.put("gender",gender);profile.put("birth_date",dob.isEmpty()?JSONObject.NULL:dob);profile.put("public_key","");request("POST","/rest/v1/profiles",profile,true,"resolution=merge-duplicates,return=minimal",token);profileSaved=true;}
                success=true;
            }catch(Exception e){error=shortError(e);}
            final boolean done=success,saved=profileSaved; final String failure=error,finalToken=token,finalAuthId=returnedId;
            handler.post(()->{clickedButton.setEnabled(true);clickedButton.setText(create?"Create account":"Sign in");if(!done){msg("કામ પૂર્ણ થયું નથી:\n"+failure);return;}if(create&&!saved){msg("પ્રોફાઇલ સેવ થઈ નથી. ફરીથી તપાસો.");return;}accessToken=finalToken;authUserId=finalAuthId;userId=raw;displayUser=raw;showHome();});
        });
    }
    private String toSyntheticEmail(String id){String encoded=Base64.encodeToString(id.toLowerCase().getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);return encoded+"@users.indianmessenger.invalid";}

    private void showHome(){content.removeAllViews();content.addView(text("નમસ્તે, "+displayUser,21,Color.rgb(28,42,54),true));content.addView(text("ચેટ શરૂ કરવા માટે સામેના વ્યક્તિનું સંપૂર્ણ User ID લખો.",14,Color.DKGRAY,false));searchField=edit("સંપૂર્ણ User ID શોધો");content.addView(searchField);Button search=button("Find exact user");content.addView(search);search.setOnClickListener(v->findUser(search));content.addView(text("આ Starter MVP છે. ચેટ મોકલવાનું અને સંપૂર્ણ E2E encryption હજી અમલમાં નથી.",13,Color.DKGRAY,false));Button logout=button("Sign out");content.addView(logout);logout.setOnClickListener(v->{accessToken="";authUserId="";userId="";displayUser="";showAuth();});}

    private void findUser(Button searchButton){final String exact=searchField.getText().toString().trim();if(!validId(exact)){msg("માન્ય સંપૂર્ણ User ID દાખલ કરો.");return;}searchButton.setEnabled(false);searchButton.setText("શોધી રહ્યા છીએ...");executor.execute(()->{JSONArray results=null;String error=null;try{String encoded=URLEncoder.encode(exact,"UTF-8");results=requestArray("GET","/rest/v1/profiles?select=user_id,public_key&user_id=eq."+encoded+"&limit=1",null,true,accessToken);}catch(Exception e){error=shortError(e);}final JSONArray found=results;final String failure=error;handler.post(()->{searchButton.setEnabled(true);searchButton.setText("Find exact user");if(failure!=null){msg("યુઝર શોધવામાં ભૂલ:\n"+failure);return;}if(found==null||found.length()==0){msg("આ User ID મળ્યું નથી.");return;}try{JSONObject person=found.getJSONObject(0);selectedRecipient=person.optString("user_id");content.removeAllViews();content.addView(text("User: "+selectedRecipient,19,Color.rgb(28,42,54),true));content.addView(text("આ Starter MVP માં સંદેશ મોકલવાની સુવિધા હજી તૈયાર નથી.",14,Color.DKGRAY,false));messageField=edit("Message");content.addView(messageField);Button send=button("Send");content.addView(send);send.setOnClickListener(v->msg("સંદેશ મોકલવાની સુવિધા હજી અમલમાં નથી."));Button home=button("Home");content.addView(home);home.setOnClickListener(v->showHome());}catch(Exception e){msg("યુઝર માહિતી વાંચવામાં ભૂલ.");}});});}

    private JSONObject request(String method,String path,JSONObject body,boolean useAuth)throws Exception{return request(method,path,body,useAuth,null,accessToken);}
    private JSONObject request(String method,String path,JSONObject body,boolean useAuth,String prefer,String token)throws Exception{HttpURLConnection c=connection(path,method,useAuth,prefer,token);try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}String response=read(c);int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code+": "+response);return response.trim().startsWith("{")?new JSONObject(response):new JSONObject();}finally{c.disconnect();}}
    private JSONArray requestArray(String method,String path,JSONObject body,boolean useAuth,String token)throws Exception{HttpURLConnection c=connection(path,method,useAuth,null,token);try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}String response=read(c);int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code+": "+response);return new JSONArray(response);}finally{c.disconnect();}}
    private HttpURLConnection connection(String path,String method,boolean useAuth,String prefer,String token)throws Exception{URL url=new URL(SUPABASE_URL+path);HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestProperty("apikey",SUPABASE_ANON_KEY);String bearer=useAuth&&token!=null&&!token.isEmpty()?token:SUPABASE_ANON_KEY;c.setRequestProperty("Authorization","Bearer "+bearer);c.setRequestProperty("Accept","application/json");if(prefer!=null)c.setRequestProperty("Prefer",prefer);return c;}
    private String read(HttpURLConnection c)throws Exception{int code=c.getResponseCode();InputStream in=(code>=200&&code<300)?c.getInputStream():c.getErrorStream();if(in==null)return "";try(InputStream input=in;ByteArrayOutputStream output=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1)output.write(buffer,0,n);return output.toString("UTF-8");}}
    private String shortError(Exception e){String s=e.getMessage();if(s==null||s.trim().isEmpty())return "અજ્ઞાત ભૂલ";return s.length()>250?s.substring(0,250):s;}
    private EditText edit(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextSize(15);e.setPadding(dp(12),dp(9),dp(12),dp(9));e.setBackground(bg(Color.WHITE,0xFFD9E1E5));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);p.bottomMargin=dp(5);e.setLayoutParams(p);return e;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(Color.rgb(25,73,80));b.setBackground(bg(Color.WHITE,0xFFD6E3E7));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(7);p.bottomMargin=dp(5);b.setLayoutParams(p);return b;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(dp(3),dp(5),dp(3),dp(5));if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable bg(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));d.setStroke(dp(1),stroke);return d;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void msg(String message){new AlertDialog.Builder(this).setTitle("Indian Messenger").setMessage(message).setPositiveButton("OK",null).show();}
}
