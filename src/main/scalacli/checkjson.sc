//> using toolkit default
import ujson.Value

val jsonPath = os.pwd / "stationinfo.json"

val stats = ujson.read(os.read(jsonPath)).arr.map(_.obj)

def valCheck(prop: String, extraCheck: Value => Boolean = _ => false) =
	() => stats.filter(s => !s.contains(prop) || extraCheck(s(prop)))

def numCheck(prop: String, extraCheck: Double => Boolean = _ => false) =
	valCheck(prop, v => extraCheck(v.num))

val checks = Seq(
	"Yearless"       -> valCheck("years", _.arr.isEmpty),
	"ID-less"        -> valCheck("id", _.str.isEmpty),
	"Altitude-less"  -> numCheck("alt", _ <= 0),
	"Latitude-less"  -> numCheck("lat"),
	"Longitude-less" -> numCheck("lon")
)

var nErrors = 0

for (msg, checkThunk) <- checks do
	try
		val baddies = checkThunk()
		if baddies.nonEmpty then
			println(s"$msg stations: $baddies")
			nErrors += 1
	catch case err =>
		println(s"Problem computing $msg stations: ${err.getMessage}")
		nErrors +=1

if nErrors == 0 then println(s"No problems found in $jsonPath")
