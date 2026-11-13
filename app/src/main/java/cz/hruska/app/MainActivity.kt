package cz.hruska.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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

private fun validGps(value: String): Boolean {
 val parts = value.trim().split(",")
 if(parts.size != 2) return false
 val lat = parts[0].trim().toDoubleOrNull() ?: return false
 val lon = parts[1].trim().toDoubleOrNull() ?: return false
 return lat in -90.0..90.0 && lon in -180.0..180.0
}

private fun reordered(stops: JSONArray, from: Int, to: Int): JSONArray {
 val list=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList()
 if(from !in list.indices || to !in list.indices) return stops
 list.add(to,list.removeAt(from))
 return JSONArray(list)
}

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
   var tripData by remember { mutableStateOf(prefs.getString("trips", "[]") ?: "[]") }
   var contactsData by remember { mutableStateOf(prefs.getString("contacts", "[]") ?: "[]") }
   var contactName by remember { mutableStateOf("") }
   var contactPhone by remember { mutableStateOf("") }
   var contactNote by remember { mutableStateOf("") }
   var contactGroup by remember { mutableStateOf("Ostatní") }
   var groupData by remember { mutableStateOf(prefs.getString("contact_groups", "[]") ?: "[]") }
   var newGroup by remember { mutableStateOf("") }
   var addingGroup by remember { mutableStateOf(false) }
   var groupExpanded by remember { mutableStateOf(false) }
   var contactEditing by remember { mutableIntStateOf(-2) }
   var contactDeleting by remember { mutableIntStateOf(-1) }
   var contactsUnlocked by remember { mutableStateOf(false) }
   var askContactsUnlock by remember { mutableStateOf(false) }
   var selectedTrip by remember { mutableIntStateOf(-1) }
   var deletingTrip by remember { mutableIntStateOf(-1) }
   var selected by remember { mutableIntStateOf(-1) }
   var edit by remember { mutableStateOf(false) }
   var unlocked by remember { mutableStateOf(false) }
   var askUnlock by remember { mutableStateOf(false) }
   var tripUnlocked by remember { mutableStateOf(false) }
   var askTripUnlock by remember { mutableStateOf(false) }
   var name by remember { mutableStateOf("") }
   var address by remember { mutableStateOf("") }
   var gps by remember { mutableStateOf("") }
   var note by remember { mutableStateOf("") }
   var query by remember { mutableStateOf("") }
   var notesMode by remember { mutableStateOf(false) }
   var addLine by remember { mutableStateOf(false) }
   var editingStop by remember { mutableIntStateOf(-1) }
   var confirmDeleteStop by remember { mutableStateOf(false) }
   val context = LocalContext.current
   val routes = remember(data) { JSONArray(data) }
   val trips = remember(tripData) { JSONArray(tripData) }
   val contacts = remember(contactsData) { JSONArray(contactsData) }
   val customGroups=remember(groupData){JSONArray(groupData)}
   val groups=remember(groupData){listOf("Dispečink","Servis","Řidič","Podnik","Ostatní")+(0 until customGroups.length()).map{customGroups.optString(it)}}
   fun saveContacts(a: JSONArray) { contactsData=a.toString();prefs.edit().putString("contacts",contactsData).apply() }
   fun saveTrips(a: JSONArray) { tripData=a.toString();prefs.edit().putString("trips",tripData).apply() }
   fun save(a: JSONArray) { data = a.toString(); prefs.edit().putString("routes", data).apply() }
   fun back() { unlocked=false;askUnlock=false;tripUnlocked=false;askTripUnlock=false;contactsUnlocked=false;askContactsUnlock=false;contactEditing=-2; if(edit) edit=false else if(screen==2) {screen=1;selected=-1;query="";unlocked=false} else if(screen==4) screen=3 else screen=0 }
   BackHandler(screen!=0 || edit) { back() }
   val colors=darkColorScheme(primary=Indigo, onPrimary=Color.White, background=Dark, onBackground=Color.White, surface=Surface, onSurface=Color.White, surfaceVariant=Color(0xFF292B39))
   MaterialTheme(colorScheme=colors) {
    Surface(modifier=Modifier.fillMaxSize(),color=Dark) {
     Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
      Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       if(screen!=0) IconButton(onClick={back()}) { Icon(Icons.Default.ArrowBack,"Zpět") }
       Text(if(screen==0) "HRUŠKA" else if(screen==1) "Vyber linku" else if(screen==2) routes.getJSONObject(selected).optString("name") else if(screen==3) "Moje trasy" else if(screen==4) trips.getJSONObject(selectedTrip).optString("name") else if(screen==6) "Kontakty" else "Koš",modifier=Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
       if(screen==0) IconButton(onClick={screen=5}) { Icon(Icons.Default.Delete,"Koš",tint=Color.White) }
       if(screen==2) {
        IconButton(onClick={edit=!edit;name=routes.getJSONObject(selected).optString("name")}) { Icon(Icons.Default.Edit,"Upravit linku") }
        IconButton(onClick={if(unlocked) unlocked=false else askUnlock=true}) { Icon(if(unlocked) Icons.Default.LockOpen else Icons.Default.Lock,if(unlocked) "Zamknout pořadí" else "Odemknout pořadí",tint=if(unlocked) Color(0xFF24C65A) else Color(0xFFE53935)) }
       }
       if(screen==6) IconButton(onClick={if(contactsUnlocked) contactsUnlocked=false else askContactsUnlock=true}) {
        Icon(if(contactsUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,if(contactsUnlocked) "Zamknout úpravy a mazání" else "Odemknout úpravy a mazání",tint=if(contactsUnlocked) Color(0xFF24C65A) else Color(0xFFE53935))
       }
       if(screen==4) IconButton(onClick={if(tripUnlocked) tripUnlocked=false else askTripUnlock=true}) {
        Icon(if(tripUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,if(tripUnlocked) "Zamknout pořadí" else "Odemknout pořadí",tint=if(tripUnlocked) Color(0xFF24C65A) else Color(0xFFE53935))
       }
      }
      when(screen) {
       0 -> {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Route,null,Modifier.align(Alignment.CenterHorizontally).size(64.dp),tint=Color.White)
        Text("Plánování rozvozových tras",modifier=Modifier.align(Alignment.CenterHorizontally),color=Color.White)
        Spacer(Modifier.weight(1f))
        Button(onClick={screen=1},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Indigo)) {
         Icon(Icons.Default.Search,"Hledat linku",tint=Color.White)
         Spacer(Modifier.width(12.dp))
         Text("Vyber linku")
        }
        Button(onClick={screen=3},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){
         Icon(Icons.Default.Map,"Mapa",tint=Color.White)
         Spacer(Modifier.width(12.dp))
         Text("Moje trasy")
        }
        Button(onClick={screen=6;contactsUnlocked=false},modifier=Modifier.fillMaxWidth().height(58.dp),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){
         Icon(Icons.Default.Person,"Kontakty",tint=Color.White)
         Spacer(Modifier.width(12.dp))
         Text("Kontakty")
        }
       }
       1 -> {
        OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Hledat linku")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items((0 until routes.length()).filter{routes.getJSONObject(it).optString("name").contains(query,true)}) { i ->
          val r=routes.getJSONObject(i)
          ElevatedCard(onClick={selected=i;screen=2;query="";edit=false;unlocked=false},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=CardDefaults.elevatedCardColors(containerColor=Surface)) {
           Column(Modifier.padding(18.dp)) {
            Text(r.optString("name"),fontWeight=FontWeight.Bold)
            Text(r.optJSONArray("stops")?.length().toString()+" zastávek",color=Color.White)
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
        if(askUnlock) AlertDialog(onDismissRequest={askUnlock=false},title={Text("Změnit pořadí adres?")},text={Text("Opravdu chcete změnit pořadí zastávek v této lince?")},confirmButton={TextButton(onClick={unlocked=true;askUnlock=false;edit=false;query=""}){Text("Ano, odemknout",color=Color(0xFFE53935))}},dismissButton={TextButton(onClick={askUnlock=false}){Text("Zrušit",color=Color.White)}})
        val r=routes.getJSONObject(selected)
        val stops=r.getJSONArray("stops")
        if(editingStop >= 0 && editingStop < stops.length()) {
         AlertDialog(
          onDismissRequest={editingStop=-1;confirmDeleteStop=false},
          title={Text(if(confirmDeleteStop) "Smazat adresu?" else "Upravit zastávku")},
          text={
           if(confirmDeleteStop) Text("Chcete opravdu adresu smazat? Tuto akci nelze vrátit zpět.")
           else Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(address,{address=it},label={Text("Adresa")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(gps,{gps=it},label={Text("GPS souřadnice (nepovinné)")},modifier=Modifier.fillMaxWidth(),singleLine=true,isError=gps.isNotBlank() && !validGps(gps))
            if(gps.isNotBlank() && !validGps(gps)) Text("Formát: 49.8209, 18.2625",color=MaterialTheme.colorScheme.error)
            OutlinedTextField(note,{note=it},label={Text("Poznámka")},modifier=Modifier.fillMaxWidth())
           }
          },
          confirmButton={
           if(confirmDeleteStop) TextButton(onClick={
            val remaining=(0 until stops.length()).filter{it!=editingStop}.map{stops.getJSONObject(it)}
            r.put("stops",JSONArray(remaining));save(routes);editingStop=-1;confirmDeleteStop=false
           }){Text("Ano, smazat",color=MaterialTheme.colorScheme.error)}
           else TextButton(onClick={
            stops.getJSONObject(editingStop).put("address",address.trim()).put("gps",gps.trim()).put("note",note)
            save(routes);editingStop=-1
           },enabled=(address.isNotBlank() || gps.isNotBlank()) && (gps.isBlank() || validGps(gps))){Text("Uložit")}
          },
          dismissButton={
           if(confirmDeleteStop) TextButton(onClick={confirmDeleteStop=false}){Text("Ne, ponechat")}
           else Row {
            TextButton(onClick={confirmDeleteStop=true}){Text("Smazat",color=MaterialTheme.colorScheme.error)}
            TextButton(onClick={editingStop=-1}){Text("Zrušit")}
           }
          }
         )
        }
        if(edit) {
         OutlinedTextField(name,{name=it},modifier=Modifier.fillMaxWidth(),label={Text("Název linky")})
         Button(onClick={r.put("name",name.trim());save(routes);edit=false},enabled=name.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Uložit název")}
         HorizontalDivider()
         OutlinedTextField(address,{address=it},modifier=Modifier.fillMaxWidth(),label={Text("Adresa zastávky (nepovinná při GPS)")})
         OutlinedTextField(gps,{gps=it},modifier=Modifier.fillMaxWidth(),label={Text("GPS souřadnice (nepovinné)")},placeholder={Text("49.8209, 18.2625")},singleLine=true,isError=gps.isNotBlank() && !validGps(gps))
         if(gps.isNotBlank() && !validGps(gps)) Text("Zadej souřadnice jako šířka, délka (např. 49.8209, 18.2625)",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
         OutlinedTextField(note,{note=it},modifier=Modifier.fillMaxWidth(),label={Text("Poznámka")})
         Button(onClick={stops.put(JSONObject().put("address",address.trim()).put("gps",gps.trim()).put("note",note).put("done",false));save(routes);address="";gps="";note=""},enabled=(address.isNotBlank() || gps.isNotBlank()) && (gps.isBlank() || validGps(gps)),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Přidat zastávku")}
        } else OutlinedTextField(query,{query=it;notesMode=false},modifier=Modifier.fillMaxWidth(),label={Text("Hledat adresu")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        val found=(0 until stops.length()).filter { i ->
         val s=stops.getJSONObject(i)
         query.isBlank() || (if(notesMode)s.optString("note") else s.optString("address")+" "+s.optString("gps")).contains(query,true)
        }
        val elsewhere=if(query.isNotBlank()) (0 until routes.length()).filter{it!=selected}.flatMap { ri ->
         val other=routes.getJSONObject(ri).getJSONArray("stops")
         (0 until other.length()).mapNotNull { si ->
          val stop=other.getJSONObject(si)
          val value=if(notesMode) stop.optString("note") else stop.optString("address")+" "+stop.optString("gps")
          if(value.contains(query,true)) Pair(ri,stop) else null
         }
        } else emptyList()
        if(!edit && query.isNotBlank() && found.isEmpty() && elsewhere.isEmpty() && !notesMode) {
         Text("Nenalezeno v žádné lince")
         OutlinedButton(onClick={notesMode=true}){Text("Prohledat poznámky všech linek")}
        }
        if(!edit && query.isNotBlank() && elsewhere.isNotEmpty()) {
         Text("Výsledky z ostatních linek")
         elsewhere.take(8).forEach { match ->
          OutlinedButton(onClick={selected=match.first;query="";notesMode=false},modifier=Modifier.fillMaxWidth()) {
           Text(routes.getJSONObject(match.first).optString("name")+": "+match.second.optString("address").ifBlank{"GPS "+match.second.optString("gps")})
          }
         }
        }
        if(!edit) LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items(found) { i ->
          val s=stops.getJSONObject(i)
          ElevatedCard(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Surface),shape=RoundedCornerShape(14.dp)) {
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
             Text((i+1).toString()+". "+(s.optString("address").ifBlank { "GPS: "+s.optString("gps") }),modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
             IconButton(onClick={editingStop=i;address=s.optString("address");gps=s.optString("gps");note=s.optString("note");confirmDeleteStop=false},modifier=Modifier.size(40.dp)) {
              Icon(Icons.Default.Edit,contentDescription="Upravit adresu",modifier=Modifier.size(19.dp),tint=Color.White)
             }
            }
            if(s.optString("address").isNotBlank() && s.optString("gps").isNotBlank()) Text("GPS: "+s.optString("gps"),color=Color.White)
            if(s.optString("note").isNotBlank()) Text(s.optString("note"),color=Color.White)
            if(unlocked && !edit && query.isBlank()) {
             var position by remember(i,data) { mutableStateOf((i+1).toString()) }
             Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
              OutlinedButton(onClick={r.put("stops",reordered(stops,i,i-1));save(routes)},enabled=i>0,contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.ArrowUpward,"Nahoru",tint=Color.White)}
              OutlinedButton(onClick={r.put("stops",reordered(stops,i,i+1));save(routes)},enabled=i<stops.length()-1,contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.ArrowDownward,"Dolů",tint=Color.White)}
              OutlinedTextField(value=position,onValueChange={v->if(v.length<=5 && v.all{it.isDigit()}) position=v},modifier=Modifier.width(78.dp),singleLine=true,label={Text("Pozice")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
              TextButton(onClick={
               val target=position.toIntOrNull()
               if(target!=null && target in 1..stops.length()) {r.put("stops",reordered(stops,i,target-1));save(routes)}
              },enabled=position.toIntOrNull() in 1..stops.length()){Text("OK")}
             }
            }
            if(edit) {
             Row {
              TextButton(onClick={if(i>0){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i-1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i>0){Text("↑")}
              TextButton(onClick={if(i<stops.length()-1){val a=(0 until stops.length()).map{stops.getJSONObject(it)}.toMutableList();val v=a.removeAt(i);a.add(i+1,v);r.put("stops",JSONArray(a));save(routes)}},enabled=i<stops.length()-1){Text("↓")}
              TextButton(onClick={val a=(0 until stops.length()).filter{it!=i}.map{stops.getJSONObject(it)};r.put("stops",JSONArray(a));save(routes)}){Text("Smazat")}
             }
            } else Row(verticalAlignment=Alignment.CenterVertically) {
             Checkbox(s.optBoolean("done"),{s.put("done",it);save(routes)})
             Button(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(s.optString("gps").ifBlank { s.optString("address") }))))},colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Navigovat")}
            }
           }
          }
         }
        }
        if(!edit && query.isBlank()) Button(onClick={
         val chosen=JSONArray()
         for(i in 0 until stops.length()) {
          val stop=stops.getJSONObject(i)
          if(stop.optBoolean("done")) chosen.put(JSONObject(stop.toString()).put("done",false))
         }
         val today=LocalDate.now()
         trips.put(JSONObject().put("name",r.optString("name")+" • "+today.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).put("date",today.toString()).put("createdAt",System.currentTimeMillis()).put("deleted",false).put("stops",chosen))
         saveTrips(trips);screen=3
        },enabled=(0 until stops.length()).any{stops.getJSONObject(it).optBoolean("done")},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Sestavit trasu")}
       }
       3,5 -> {
        val trash=screen==5
        val indices=(0 until trips.length()).filter{trips.getJSONObject(it).optBoolean("deleted")==trash}.sortedWith(compareByDescending<Int>{trips.getJSONObject(it).optString("date")}.thenByDescending{trips.getJSONObject(it).optLong("createdAt")})
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
         items(indices){i->
          val trip=trips.getJSONObject(i)
          ElevatedCard(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Surface)){
           Column(Modifier.padding(14.dp)){
            Text(trip.optString("name"),fontWeight=FontWeight.Bold)
            Text(trip.getJSONArray("stops").length().toString()+" zastávek",color=Color.White)
            Row {
             if(trash) TextButton(onClick={trip.put("deleted",false);saveTrips(trips)}){Text("Obnovit")}
             else {
              TextButton(onClick={selectedTrip=i;screen=4;tripUnlocked=false}){Text("Otevřít",color=Color.White)}
              TextButton(onClick={deletingTrip=i}){Text("Smazat",color=Color(0xFFE53935))}
             }
            }
           }
          }
         }
        }
        if(deletingTrip>=0 && !trash) AlertDialog(onDismissRequest={deletingTrip=-1},title={Text("Přesunout do koše?")},text={Text("Trasu bude možné obnovit.")},confirmButton={TextButton(onClick={trips.getJSONObject(deletingTrip).put("deleted",true);saveTrips(trips);deletingTrip=-1}){Text("Přesunout")}},dismissButton={TextButton(onClick={deletingTrip=-1}){Text("Zrušit")}})
       }
       6 -> {
        if(askContactsUnlock) AlertDialog(onDismissRequest={askContactsUnlock=false},title={Text("Odemknout kontakty?")},text={Text("Opravdu chcete povolit úpravy a mazání kontaktů?")},confirmButton={TextButton(onClick={contactsUnlocked=true;askContactsUnlock=false}){Text("Ano, odemknout",color=Color(0xFFE53935))}},dismissButton={TextButton(onClick={askContactsUnlock=false}){Text("Zrušit",color=Color.White)}})
        if(contactEditing>=-1) AlertDialog(onDismissRequest={contactEditing=-2},title={Text(if(contactEditing==-1) "Nový kontakt" else "Upravit kontakt")},text={
         Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
          OutlinedTextField(contactName,{contactName=it},label={Text("Jméno")},modifier=Modifier.fillMaxWidth())
          OutlinedTextField(contactPhone,{contactPhone=it},label={Text("Telefon")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.fillMaxWidth())
          OutlinedTextField(contactNote,{contactNote=it},label={Text("Poznámka")},modifier=Modifier.fillMaxWidth())
          Box {
           OutlinedButton(onClick={groupExpanded=true},modifier=Modifier.fillMaxWidth()){Text("Skupina: "+contactGroup,color=Color.White)}
           DropdownMenu(expanded=groupExpanded,onDismissRequest={groupExpanded=false}){
            groups.forEach { g ->
             DropdownMenuItem(text={Text(g)},onClick={contactGroup=g;groupExpanded=false})
            }
           }
          }
         }
        },confirmButton={TextButton(onClick={
         val item=JSONObject().put("name",contactName.trim()).put("phone",contactPhone.trim()).put("note",contactNote.trim()).put("group",contactGroup)
         if(contactEditing==-1) contacts.put(item) else contacts.put(contactEditing,item)
         saveContacts(contacts);contactEditing=-2
        },enabled=contactName.isNotBlank()){Text("Uložit",color=Color.White)}},dismissButton={TextButton(onClick={contactEditing=-2}){Text("Zrušit",color=Color.White)}})
        if(contactDeleting>=0) AlertDialog(onDismissRequest={contactDeleting=-1},title={Text("Smazat kontakt?")},text={Text("Opravdu chcete tento kontakt trvale smazat?")},confirmButton={TextButton(onClick={
         val remaining=JSONArray((0 until contacts.length()).filter{it!=contactDeleting}.map{contacts.getJSONObject(it)})
         saveContacts(remaining);contactDeleting=-1
        }){Text("Ano, smazat",color=Color(0xFFE53935))}},dismissButton={TextButton(onClick={contactDeleting=-1}){Text("Zrušit",color=Color.White)}})
        if(addingGroup) AlertDialog(onDismissRequest={addingGroup=false},title={Text("Nová skupina")},text={OutlinedTextField(newGroup,{newGroup=it},label={Text("Název skupiny")})},confirmButton={TextButton(onClick={
         val candidate=newGroup.trim()
         if(candidate.isNotBlank() && groups.none{it.equals(candidate,true)}) {
          customGroups.put(candidate);groupData=customGroups.toString();prefs.edit().putString("contact_groups",groupData).apply()
         }
         newGroup="";addingGroup=false
        },enabled=newGroup.isNotBlank() && groups.none{it.equals(newGroup.trim(),true)}){Text("Přidat",color=Color.White)}},dismissButton={TextButton(onClick={addingGroup=false}){Text("Zrušit",color=Color.White)}})
        OutlinedButton(onClick={newGroup="";addingGroup=true},modifier=Modifier.fillMaxWidth()){Text("+ Přidat skupinu",color=Color.White)}
        Button(onClick={contactName="";contactPhone="";contactNote="";contactGroup="Ostatní";contactEditing=-1},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("+ Nový kontakt")}
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
         items((0 until contacts.length()).sortedBy{contacts.getJSONObject(it).optString("name").lowercase()}){i->
          val contact=contacts.getJSONObject(i)
          ElevatedCard(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Surface)){
           Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Text(contact.optString("name"),fontWeight=FontWeight.Bold,color=Color.White)
            Text("Skupina: "+contact.optString("group","Ostatní"),color=Color.White)
            if(contact.optString("phone").isNotBlank()) Text(contact.optString("phone"),color=Color.White)
            if(contact.optString("note").isNotBlank()) Text(contact.optString("note"),color=Color.White)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
             if(contact.optString("phone").isNotBlank()) {
              IconButton(onClick={context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(contact.optString("phone")))))},modifier=Modifier.offset(x=(-12).dp)){Icon(Icons.Default.Phone,"Zavolat",tint=Color(0xFF24C65A))}
              IconButton(onClick={context.startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(contact.optString("phone")))))},modifier=Modifier.offset(x=(-12).dp)){Icon(Icons.Default.Sms,"Napsat SMS",tint=Color(0xFF69C9FF))}
             }
             if(contactsUnlocked) TextButton(onClick={contactName=contact.optString("name");contactPhone=contact.optString("phone");contactNote=contact.optString("note");contactGroup=contact.optString("group","Ostatní");contactEditing=i}){Text("Upravit",color=Color.White)}
             if(contactsUnlocked) TextButton(onClick={contactDeleting=i}){Text("Smazat",color=Color(0xFFE53935))}
            }
           }
          }
         }
        }
       }
       4 -> {
        if(askTripUnlock) AlertDialog(onDismissRequest={askTripUnlock=false},title={Text("Změnit pořadí adres?")},text={Text("Opravdu chcete změnit pořadí zastávek v této trase?")},confirmButton={TextButton(onClick={tripUnlocked=true;askTripUnlock=false}){Text("Ano, odemknout",color=Color(0xFFE53935))}},dismissButton={TextButton(onClick={askTripUnlock=false}){Text("Zrušit",color=Color.White)}})
        val tripStops=trips.getJSONObject(selectedTrip).getJSONArray("stops")
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
         items((0 until tripStops.length()).toList()){i->
          val stop=tripStops.getJSONObject(i)
          ElevatedCard(modifier=Modifier.fillMaxWidth(),colors=CardDefaults.elevatedCardColors(containerColor=Surface)){
           Column(Modifier.padding(14.dp)){
            Text((i+1).toString()+". "+stop.optString("address").ifBlank{"GPS: "+stop.optString("gps")},fontWeight=FontWeight.Bold)
            if(stop.optString("note").isNotBlank()) Text(stop.optString("note"))
            if(tripUnlocked) {
             var position by remember(i,tripData) { mutableStateOf((i+1).toString()) }
             Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
              OutlinedButton(onClick={trips.getJSONObject(selectedTrip).put("stops",reordered(tripStops,i,i-1));saveTrips(trips)},enabled=i>0,contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.ArrowUpward,"Nahoru",tint=Color.White)}
              OutlinedButton(onClick={trips.getJSONObject(selectedTrip).put("stops",reordered(tripStops,i,i+1));saveTrips(trips)},enabled=i<tripStops.length()-1,contentPadding=PaddingValues(horizontal=10.dp)){Icon(Icons.Default.ArrowDownward,"Dolů",tint=Color.White)}
              OutlinedTextField(value=position,onValueChange={v->if(v.length<=5 && v.all{it.isDigit()}) position=v},modifier=Modifier.width(78.dp),singleLine=true,label={Text("Pozice")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
              TextButton(onClick={val target=position.toIntOrNull();if(target!=null && target in 1..tripStops.length()){trips.getJSONObject(selectedTrip).put("stops",reordered(tripStops,i,target-1));saveTrips(trips)}},enabled=position.toIntOrNull() in 1..tripStops.length()){Text("OK",color=Color.White)}
             }
            }
            Row(verticalAlignment=Alignment.CenterVertically){
             Checkbox(stop.optBoolean("done"),{stop.put("done",it);saveTrips(trips)})
             Button(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.google.com/maps/dir/?api=1&destination="+Uri.encode(stop.optString("gps").ifBlank{stop.optString("address")}))))},colors=ButtonDefaults.buttonColors(containerColor=Indigo)){Text("Navigovat")}
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