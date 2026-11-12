package cz.hruska.app
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val prefs=getSharedPreferences("data",MODE_PRIVATE)
  setContent {
   var data by remember { mutableStateOf(prefs.getString("routes","[]") ?: "[]") }
   var selected by remember { mutableIntStateOf(-1) }
   var edit by remember { mutableStateOf(false) }
   var name by remember { mutableStateOf("") }
   var address by remember { mutableStateOf("") }
   var note by remember { mutableStateOf("") }
   var query by remember { mutableStateOf("") }
   var notesMode by remember { mutableStateOf(false) }
   val context=LocalContext.current
   val routes=remember(data){JSONArray(data)}
   fun save(a:JSONArray){data=a.toString();prefs.edit().putString("routes",data).apply()}
   fun route():JSONObject=routes.getJSONObject(selected)
   MaterialTheme {
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
     Text("HRUŠKA",style=MaterialTheme.typography.headlineMedium)
     if(selected<0){
      Text("Vyber linku",style=MaterialTheme.typography.titleLarge)
      for(i in 0 until routes.length()){
       val r=routes.getJSONObject(i)
       OutlinedButton(onClick={selected=i;edit=false;query=""},modifier=Modifier.fillMaxWidth()){Text(r.optString("name"))}
      }
      OutlinedTextField(name,{name=it},label={Text("Nová linka")})
      Button(onClick={routes.put(JSONObject().put("name",name).put("stops",JSONArray()));save(routes);name=""},enabled=name.isNotBlank()){Text("Přidat linku")}
     } else {
      val r=route()
      Row {
       TextButton(onClick={selected=-1;edit=false}){Text("← Zpět")}
       Text(r.optString("name"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
       TextButton(onClick={edit=!edit;name=r.optString("name")}){Text("✎")}
      }
      if(edit){
       OutlinedTextField(name,{name=it},label={Text("Název linky")})
       Button(onClick={r.put("name",name);save(routes);edit=false},enabled=name.isNotBlank()){Text("Uložit název")}
       HorizontalDivider()
       OutlinedTextField(address,{address=it},label={Text("Adresa zastávky")})
       OutlinedTextField(note,{note=it},label={Text("Poznámka")})
       Button(onClick={r.getJSONArray("stops").put(JSONObject().put("address",address).put("note",note).put("done",false));save(routes);address="";note=""},enabled=address.isNotBlank()){Text("Přidat zastávku")}
      } else {
       OutlinedTextField(query,{query=it;notesMode=false},label={Text("Hledat adresu")},modifier=Modifier.fillMaxWidth())
      }
      val stops=r.getJSONArray("stops")
      val found=(0 until stops.length()).filter{ i->
       val s=stops.getJSONObject(i)
       query.isBlank() || (if(notesMode)s.optString("note") else s.optString("address")).contains(query,true)
      }
      if(!edit && query.isNotBlank() && found.isEmpty() && !notesMode){
       Text("Adresa nenalezena")
       Button(onClick={notesMode=true}){Text("Prohledat poznámky")}
      }
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
       items(found){i->
        val s=stops.getJSONObject(i)
        ElevatedCard(Modifier.fillMaxWidth()){
         Column(Modifier.padding(12.dp)){
          Text((i+1).toString()+". "+s.optString("address"))
          Text(s.optString("note"))
          if(edit){
           Row{
            TextButton(onClick={if(i>0){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i-1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i>0){Text("↑")}
            TextButton(onClick={if(i<stops.length()-1){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i+1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i<stops.length()-1){Text("↓")}
            TextButton(onClick={val a=(0 until stops.length()).filter{it!=i}.map{stops.getJSONObject(it)};r.put("stops",JSONArray(a));save(routes)}){Text("Smazat")}
           }
          } else {
           Row{
            Checkbox(s.optBoolean("done"),{s.put("done",it);save(routes)})
            TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(s.optString("address")))))}){Text("Navigovat")}
           }
          }
         }
        }
       }
      }
     }
    }
   }
  }
 }
}