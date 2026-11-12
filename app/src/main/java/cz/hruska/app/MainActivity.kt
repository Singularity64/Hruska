package cz.hruska.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private val Indigo = Color(0xFF1B1EA1)
private val Dark = Color(0xFF101116)
private val Surface = Color(0xFF1C1E28)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs = getSharedPreferences("data", MODE_PRIVATE)
  setContent {
   var data by remember { mutableStateOf(prefs.getString("routes", "[]") ?: "[]") }
   var screen by remember { mutableIntStateOf(0) }
   var selected by remember { mutableIntStateOf(-1) }
   var edit by remember { mutableStateOf(false) }
   var name by remember { mutableStateOf("") }
   var address by remember { mutableStateOf("") }
   var note by remember { mutableStateOf("") }
   var query by remember { mutableStateOf("") }
   var notesMode by remember { mutableStateOf(false) }
   var addLine by remember { mutableStateOf(false) }
   val context = LocalContext.current
   val routes = remember(data) { JSONArray(data) }
   fun save(a: JSONArray) { data = a.toString(); prefs.edit().putString("routes", data).apply() }
   fun back() { if(edit) edit=false else if(screen==2) {screen=1;selected=-1;query=""} else screen=0 }
   BackHandler(screen!=0 || edit) { back() }
   val colors=darkColorScheme(primary=Indigo, onPrimary=Color.White, background=Dark, onBackground=Color.White, surface=Surface, onSurface=Color.White, surfaceVariant=Color(0xFF292B39))
   MaterialTheme(colorScheme=colors) {
    Surface(modifier=Modifier.fillMaxSize(),color=Dark) {
     Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       if(screen!=0) IconButton(onClick={back()}) { Icon(Icons.Default.ArrowBack,"Zpět") }
       Text(if(screen==0) "HRUŠKA" else if(screen==1) "Vyber linku" else routes.getJSONObject(selected).optString("name"),modifier=Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
       if(screen==2) IconButton(onClick={edit=!edit;name=routes.getJSONObject(selected).optString("name")}) { Icon(Icons.Default.Edit,"Upravit linku") }
      }
      when(screen) {
       0 -> {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Route,null,Modifier.align(Alignment.CenterHorizontally).size(64.dp),tint=Color(0xFF9295FF))
        Text("Plánování rozvozových tras",modifier=Modifier.align(Alignment.CenterHorizontally),color=Color.LightGray)
        Spacer(Modifier.weight(1f))
        Button(onClick={screen=1},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Indigo)) { Text("Vyber linku") }
       }
       1 -> {
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Hledat linku")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items((0 until routes.length()).filter{routes.getJSONObject(it).optString("name").contains(query,true)}) { i ->
          val r=routes.getJSONObject(i)
          ElevatedCard(onClick={selected=i;screen=2;query="";edit=false},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.elevatedCardColors(containerColor=Surface)) {
           Column(Modifier.padding(18.dp)) {
            Text(r.optString("name"),fontWeight=FontWeight.Bold)
            Text(r.optJSONArray("stops")?.length().toString()+" zastávek",color=Color.LightGray)
           }
          }
         }
        }
        if(addLine) {
         OutlinedTextField(name,{name=it},modifier=Modifier.fillMaxWidth(),label={Text("Název nové linky")})
         Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
          OutlinedButton(onClick={addLine=false}){Text("Zrušit")}
          Button(onClick={routes.put(JSONObject().put("name",name.trim()).put("stops",JSONArray()));save(routes);name="";addLine=false},enabled=name.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Uložit")}
         }
        } else Button(onClick={name="";addLine=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("+ Přidat linku")}
       }
       2 -> {
        val r=routes.getJSONObject(selected)
        val stops=r.getJSONArray("stops")
        if(edit) {
         OutlinedTextField(name,{name=it},modifier=Modifier.fillMaxWidth(),label={Text("Název linky")})
         Button(onClick={r.put("name",name.trim());save(routes);edit=false},enabled=name.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Uložit název")}
         HorizontalDivider()
         OutlinedTextField(address,{address=it},modifier=Modifier.fillMaxWidth(),label={Text("Adresa zastávky")})
         OutlinedTextField(note,{note=it},modifier=Modifier.fillMaxWidth(),label={Text("Poznámka")})
         Button(onClick={stops.put(JSONObject().put("address",address.trim()).put("note",note).put("done",false));save(routes);address="";note=""},enabled=address.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Přidat zastávku")}
        } else OutlinedTextField(query,{query=it;notesMode=false},modifier=Modifier.fillMaxWidth(),label={Text("Hledat adresu")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        val found=(0 until stops.length()).filter { i ->
         val s=stops.getJSONObject(i)
         query.isBlank() || (if(notesMode)s.optString("note") else s.optString("address")).contains(query,true)
        }
        if(!edit && query.isNotBlank() && found.isEmpty() && !notesMode) {
         Text("Adresa nenalezena")
         OutlinedButton(onClick={notesMode=true}){Text("Prohledat poznámky")}
        }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items(found) { i ->
          val s=stops.getJSONObject(i)
          ElevatedCard(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Surface),shape=RoundedCornerShape(14.dp)) {
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text((i+1).toString()+". "+s.optString("address"),fontWeight=FontWeight.Bold)
            if(s.optString("note").isNotBlank()) Text(s.optString("note"),color=Color.LightGray)
            if(edit) {
             Row {
              TextButton(onClick={if(i>0){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i-1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i>0){Text("↑")}
              TextButton(onClick={if(i<stops.length()-1){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i+1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i<stops.length()-1){Text("↓")}
              TextButton(onClick={val a=(0 until stops.length()).filter{it!=i}.map{stops.getJSONObject(it)};r.put("stops",JSONArray(a));save(routes)}){Text("Smazat")}
             }
            } else Row(verticalAlignment=Alignment.CenterVertically) {
             Checkbox(s.optBoolean("done"),{s.put("done",it);save(routes)})
             Button(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(s.optString("address")))))},colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Navigovat")}
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
}