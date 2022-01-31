package tk.cyriellecentori.CritiBot;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import tk.cyriellecentori.CritiBot.BotCommand.Alias;
import tk.cyriellecentori.CritiBot.Ecrit.Status;
import tk.cyriellecentori.CritiBot.Ecrit.Type;

/**
 * 
 * @author cyrielle
 * Classe principale du bot.
 */
public class CritiBot implements EventListener {
	
	public static void main(String[] args) {
		if(args.length < 1) { // Le token du bot se place en premier paramètre au lancement.
			System.err.println("Merci d'indiquer le token du bot en paramètre.");
			return;
		}
		CritiBot cb = new CritiBot(args[0]);

	}
	
	/**
	 * Date de la dernière vérification des flux RSS.
	 */
	private long lastCheck = 1620570510854L;
	/**
	 * Liste des entrées que le bot n'a pas pu interpréter.
	 */
	private Vector<SyndEntry> inbox = new Vector<SyndEntry>();
	/**
	 * Le token du bot pour se connecter à l'utilisateur Critibot.
	 */
	private String token;
	
	private JDABuilder builder;
	public JDA jda;
	
	private Gson gson;
	
	/**
	 * L'historique des états, permet de faire des retours en arrière.
	 */
	private Stack<Vector<Ecrit>> cancel = new Stack<Vector<Ecrit>>();
	/**
	 * La base de données des écrits.
	 */
	private Vector<Ecrit> ecrits;
	/**
	 * S'il y a eu une erreur lors d'une tentative de sauvegarde automatique de la base de données.
	 */
	private boolean errorSave = false;
	/**
	 * Liste des commandes.
	 */
	private LinkedHashMap<String, BotCommand> commands = new LinkedHashMap<String, BotCommand>();
	
	private Affichan[] affichans;
	
	/**
	 * L'emote de réservation.
	 */
	public final long henritueur;
	/**
	 * L'emote indiquant l'écrit comme critiqué.
	 */
	public final String henricheck = "U+1f4e8";
	/**
	 * L'emote indiquant l'écrit comme refusé.
	 */
	public final String henricross = "U+274c";
	//private final String whiteCheckBox = "U+2705";
	/**
	 * L'emoji cadenas ouvert.
	 */
	public final String unlock = "U+1f513";
	//private final String cross = "U+274e";
	/**
	 * Le préfixe du bot.
	 */
	private final String prefix;
	/**
	 * La date de la dernière mise à jour manuelle du bot.
	 */
	public long lastUpdate;
	
	/**
	 * Le salon « Organisation »
	 */
	public final long organichan;
	
	public CritiBot(String token) {
		this.token = token;
		if(token.hashCode() == 1973164890) { // Si le bot est connecté à l'utilisateur Critibot#8684, les salons sont ceux du discord des Critiqueurs.
			prefix = "c";
			henritueur = 844249814799351838L;
			organichan = 614947463610236939L;
			affichans = new Affichan[] {
					new Affichan(843956373103968308L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, null),
					new Affichan(614947463610236939L, new Status[] {Status.OUVERT_PLUS}, null, null),
					new Affichan(896361827884220467L, new Status[] {Status.INCONNU, Status.INFRACTION}, null, null),
					new Affichan(896362452818747412L, null, new Type[] {Type.AUTRE}, null),
					new Affichan(554998005850177556L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, new String[] {"Concours", "Validation"})
			};
		} else { // Sinon, le bot est considéré comme étant en bêta et les salons sont ceux de BrainBot's lair.
			prefix = "bc";
			henritueur = 470138432723877888L;
			organichan = 737725144390172714L;
			affichans = new Affichan[] {
					new Affichan(878917114474410004L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, null),
					new Affichan(737725144390172714L, new Status[] {Status.OUVERT_PLUS}, null, null),
					new Affichan(896361827884220467L, new Status[] {Status.INCONNU, Status.INFRACTION}, null, null),
					new Affichan(896362452818747412L, null, new Type[] {Type.AUTRE}, null),
					new Affichan(901072726456930365L, new Status[] {Status.OUVERT, Status.OUVERT_PLUS}, null, new String[] {"Concours", "Validation"})
			};
			System.out.println("Booting in beta.");
		}
		
		// Initialisation de l'API
		try {

			builder = JDABuilder.createDefault(this.token)
					.addEventListeners(this);

			jda = builder.build();
		} catch(LoginException | IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		jda.setAutoReconnect(true);
		
		// Initialisation du moteur JSON
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.setPrettyPrinting(); // Histoire d'avoir un beau JSON

		gson =gsonBilder.create();
		
		// Lecture de la base de données
		String data = "";
		BufferedReader dataFile;
		try {
			dataFile = new BufferedReader(new FileReader("critibot.json"));
			while(true) {
				String str = dataFile.readLine();
				if(str == null) break;
				else data = data + "\n" + str;
			}

			dataFile.close();
			
		} catch (IOException exp) {
			exp.printStackTrace();
			System.out.println("Impossible d'ouvrir les données, de nouvelles seront crées à la prochaine sauvegarde.");
			ecrits = new Vector<Ecrit>();
		}
		
		TypeToken<Vector<Ecrit>> ttve = new TypeToken<Vector<Ecrit>>() {};
		
		// À la fin du fichier se trouve normalement la date de la dernière recherche de mise à jour des flux RSS
		try {
			lastCheck = Long.parseLong(data.split("θ")[1]);
			ecrits = gson.fromJson(data.split("θ")[0], ttve.getType());
		} catch(Exception e) { // Si ce n'est pas le cas, tant pis
			ecrits = gson.fromJson(data, ttve.getType());
		}
		
		
		
		// S'il y a une erreur dans l'initialisation des données, éviter les NullPointerException
		if(ecrits == null)
			ecrits = new Vector<Ecrit>();
		
		// Vérification que tous les écrits sont bien, toujours pour éviter les NullPointerException
		for(Ecrit e : ecrits) {
			e.check();
		}
		
		// Bout de code qui supprime un nombre aléatoire d'écrits de la BDD (50% en moyenne).
		/*Vector<Ecrit> nouveau = new Vector<Ecrit>();
		java.util.Random r = new java.util.Random();
		for(Ecrit e : ecrits) {
			if(r.nextBoolean()) {
				nouveau.add(e);
			}
		}
		ecrits = nouveau;
		try {
			save();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.exit(0);*/
		
		initCommands();
		
		lastUpdate = System.currentTimeMillis();

	}
	
	/**
	 * Met à jour la BDD avec les flux RSS.
	 * @throws IllegalArgumentException
	 * @throws MalformedURLException
	 * @throws FeedException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public void addNew() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		// Récupère le flux
		SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/ct-656675.xml")));
		// Date du flux le plus récent
		Date lastDate = new Date(0);
		// Vérification de toutes les entrées
		for(Object e : feed.getEntries()) {
			SyndEntry entry = (SyndEntry) e;
			
			if(entry.getPublishedDate().after(new Date(lastCheck))) { // Vérifie si le flux est nouveau
				if(entry.getTitle().contains("]")) { // Vérifie si le titre a des balises
					Type type = null;
					// Récupère la liste des balises
					String[] sp = entry.getTitle().split("]");
					String balises = "";
					for(int i = 0; i < sp.length - 1; i++) {
						balises += sp[i] + "]";
					}
					// Nettoyage du titre (retire les SCP-XXX-FR devant, les espaces inutiles et autres trucs pas jolis).
					String unclean = sp[sp.length - 1];
					if(unclean.contains("SCP") && unclean.contains("FR")) {
						unclean = unclean.split("FR",2)[1];
					}
					while(unclean.startsWith(" ") || unclean.startsWith(":")) {
						unclean = unclean.substring(1);
					}
					if(unclean.startsWith("\"")) {
						unclean = unclean.substring(1, unclean.length() - 1);
					}
					// Si pas de titre propre, l'idée devient un « sans nom ».
					if(unclean.isEmpty() || sp.length == 1) {
						Vector<Ecrit> es = searchEcrit("sans nom");
						unclean = "(sans nom " + es.size() + ")";
						balises = entry.getTitle();
					}
					// Tentative de déduction du type.
					if(balises.contains("Idée") || balises.contains("idée")) {
						type = Type.IDEE;
					} else if(balises.contains("Conte") || balises.contains("Série") || balises.contains("conte")) {
						type = Type.CONTE;
					} else if(balises.contains("Refus")) { // Si l'idée est créée avec une balise « Refus », wtf
						type = Type.AUTRE;
					} else { // Par défaut, juste « Critiques » c'est un rapport.
						type = Type.RAPPORT;
					}
					
					// Crée l'écrit et l'ajoute à la BDD
					Ecrit ecrit = new Ecrit(unclean, entry.getLink(), type, Status.OUVERT, ((ArrayList<org.jdom.Element>) entry.getForeignMarkup()).get(0).getText());
					ecrits.add(ecrit);
					
				} else { // Si pas de balises, déjà PUTAIN LES BALISES BORDEL, ensuite ajoute à la inbox.
					inbox.add(entry);
					jda.getPresence().setStatus(OnlineStatus.IDLE); // Le signale par un statut orange.
				}
			}
			if(lastDate.before(entry.getPublishedDate())) // Garde la date la plus récente des fils regardés.
				lastDate = entry.getPublishedDate();
		}
		// Met à jour la date de dernière vérification des flux
		if(lastDate.getTime() != 0) {
			lastCheck = lastDate.getTime() + 1;
		}
	}
	
	/**
	 * Sauvegarde la BDD.
	 * @throws IOException
	 */
	public void save() throws IOException {
		BufferedWriter dataFile = new BufferedWriter(new FileWriter("critibot.json"));
		dataFile.write(gson.toJson(ecrits) + "θ" + lastCheck);
		dataFile.close();
	}
	
	/**
	 * Rend les chaînes de caractères plus « basiques » en supprimant les majuscules et les diacritiques.
	 * @param s La chaîne à simplifier.
	 * @return Une chaîne de caractères simple, seulement en minuscules sans diacritiques.
	 */
	static private String basicize(String s) {
		return s.toLowerCase()
				.replaceAll("é", "e")
				.replaceAll("ê", "e")
				.replaceAll("à", "a")
				.replaceAll("ï", "i")
				.replaceAll("ç", "c")
				.replaceAll("ë", "e")
				.replaceAll("ô", "o")
				.replaceAll("è", "e")
				.replaceAll("î", "i")
				.replaceAll("œ", "oe")
				.replaceAll("æ", "ae")
				.replaceAll("û", "u");
		
	}
	
	static public Vector<Integer> search(String s, Vector<String> content) {
		Vector<Integer> ret = new Vector<Integer>();
		String[] motsSearch = basicize(s).split(" ");
		for(int i = 0; i < content.size(); i++) { // Vérifie si chaque chaîne de caractères respecte le critère.
			String[] mots = basicize(content.get(i)).split(" "); // Simplifie la chaîne et sépare ses mots.
			boolean ok = true;
			for(String motSearch : motsSearch) { // Vérifie si un mot contient un mot du critère
				boolean found = false;
				for(String mot : mots) { 
					if(mot.toLowerCase().contains(motSearch.toLowerCase())) {
						found = true;
					}
				}
				if(!found) { // Si un mot du critère n'est pas présent dans un mot de la chaîne, alors elle ne correspond pas au critère.
					ok = false;
					break;
				}
			}
			if(ok) {
				ret.add(i);
			}
		}
		return ret;
	}
	
	/**
	 * Lance une recherche parmi les écrits. Le critère doit être consituée de parties de mots du titre, majuscules et diacritiques ignorées.
	 * @param s Le critère de recherche.
	 * @return Une liste d'écrits correspondant au critère.
	 */
	public Vector<Ecrit> searchEcrit(String s) {
		Vector<Ecrit> list = new Vector<Ecrit>();
		// Crée un tableau des noms des écrits
		Vector<String> names = new Vector<String>();
		for(Ecrit e : ecrits) {
			names.add(e.getNom());
		}
		// Récupère les indices des écrits correspondant aux critère et les ajoute au tableau de retour
		for(int index : search(s, names)) {
			list.add(ecrits.get(index));
		}
		return list;
	}
	
	/**
	 * Lance une recherche d'auteurs. Le critère doit être consituée de parties de mots du nom de l'auteur, majuscules et diacritiques ignorées.
	 * @param s Le critère de recherche.
	 * @return Une liste d'auteurs correspondant au critère.
	 */
	public Vector<String> autSearch(String s) {
		Vector<String> list = new Vector<String>();
		// Récupère la liste de tous les auteurs de la base de données.
		Vector<String> auteurList = new Vector<String>();
		for(Ecrit e : ecrits) {
			if(!auteurList.contains(e.getAuteur())) {
				auteurList.add(e.getAuteur());
			}
		}
		// Ajoute les auteurs correspondant au critère dans la liste des résultats
		for(int index : search(s, auteurList)) {
			list.add(auteurList.get(index));
		}
		return list;
	}
	
	/**
	 * Nettoie la base de données en supprimant tous les écrits considérés comme « morts » (publiés, refusés ou abandonnés).
	 * @param fort Si `true`, supprime également les écrits sans nouvelles.
	 */
	public void clean(boolean fort) {
		// Liste des écrits à supprimer.
		Vector<Ecrit> toRem = new Vector<Ecrit>();
		for(Ecrit e : ecrits) { // Vérifie chaque écrit.
			if(e.isDead() || (fort && e.getStatus() == Status.SANS_NOUVELLES))
				toRem.add(e);
		}
		// Supprime les écrits à supprimer.
		for(Ecrit e : toRem) {
			ecrits.remove(e);
		}
	}
	
	/**
	 * Supprime l'écrit donné de la base de données.
	 * @param e Écrit à supprimer.
	 */
	public void remove(Ecrit e) {
		ecrits.remove(e);
	}
	
	/**
	 * Vérifie que tous les écrits ouverts et réservés soient présents dans leur salon dédié.
	 */
	public void updateOpen() {
		for(Affichan aff : affichans) {
			aff.update(this);
		}
		for(Ecrit e : ecrits) {
			e.edited = false;
		}
	}
	
	/**
	 * Supprime tous les messages des écrits. updateOpen() étant appelé à chaque commande, tous les messages seront recrées juste après.
	 */
	public void refreshMessages() {
		for(Affichan aff : affichans) {
			aff.purge(jda);
			aff.update(this);
		}
	}
	
	/**
	 * Méthode de fusion pour le tri fusion qui suit.
	 */
	public Vector<Ecrit> merge(Vector<Ecrit> a, Vector<Ecrit> b) {
		Vector<Ecrit> res = new Vector<Ecrit>();
		int i = 0,j = 0;
		while(i < a.size() && j < b.size()) {
			if(a.get(i).getLastUpdateLong() < b.get(j).getLastUpdateLong()) {
				res.add(a.get(i));
				i++;
			} else {
				res.add(b.get(j));
				j++;
			}
		}
		for(; i < a.size(); i++) {
			res.add(a.get(i));
		}
		for(; j < b.size(); j++) {
			res.add(b.get(j));
		}
		return res;
	}
	
	/**
	 * Trie les écrits par ordre croissant de date de dernière modification.
	 * Tri utilisé : tri fusion.
	 */
	public Vector<Ecrit> sortByDate(Vector<Ecrit> toSort) {
		if(toSort.size() < 2)
			return toSort;
		return merge(sortByDate(new Vector<Ecrit>(toSort.subList(0, toSort.size() / 2))), 
				sortByDate(new Vector<Ecrit>(toSort.subList(toSort.size() / 2, toSort.size()))));
	}
	
	/**
	 * Initialise et liste les commandes.
	 */
	private void initCommands() {
		commands.put("aide", new BotCommand() {
			public void execute(CritiBot bb, MessageReceivedEvent message, String[] args) {
				EmbedBuilder b = new EmbedBuilder();
				b.setTitle("Aide de Critibot");
				b.setDescription("Les paramètres entre crochets sont optionnels, entre accolades obligatoires.");
				b.addField("Valeurs de Statut", "Statut doit être « Ouvert — Ouvert* — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé — Infraction ». « Ouvert* » correspond aux écrits ouverts possédant des marques d'intérêt, il ne peut être assigné manuellement.", false);
				b.addField("Valeurs de Type", "Type doit être « Conte — Rapport — Idée — Autre »", false);
				b.addField("Commandes de base", "`c!aide` : Cette commande d'aide.\n"
						+ "`c!annuler` : Annule la dernière modification effectuée.", false);
				b.addField("Commandes de gestion et d'affichage de la liste", "`c!ajouter {Nom};{Auteur};{Type};{Statut};{URL}` : Ajoute manuellement un écrit à la liste.\n"
						+ "`c!supprimer {Critère}` : Supprime un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit. __**ATTENTION**__ : Il n'y a pas de confirmation, faites attention à ne pas vous tromper dans le Critère.\n"
						+ "`c!inbox` : Affiche la boîte de réception, contenant les écrits qui n'ont pas pu être ajoutés automatiquement. Attention, l'appel à cette commande supprime le contenu de la boîte.", false);
				b.addField("Commandes de recherche", "`c!rechercher {Critère}` : Affiche tous les écrits contenant {Critère}.\n"
						+ "`c!lister {Statut};[Type]` : Affiche la liste des écrits avec le statut et du type demandés. Statut et Type peuvent prendre la valeur « Tout ».\n"
						+ "`c!lister_tags` : Affiche tous les tags existants dans la base de données et le nombre d'écrits y étant associés", false);
				b.addField("Commandes de critiques", "`c!marquer {Critère}` : Ajoute une marque d'intérêt à un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!marquer_pour {Critère};{Nom}` : Marque d'intérêt un écrit pour quelqu'un d'autre. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!libérer {Critère}` : Supprime votre marque d'intétêt sur un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!libérer_pour {Critère};{Nom}` : Supprime l'intêret de qulequ'un sur un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!up {Critère}` : Marque un écrit ouvert et le remet au premier plan dans le salon des fils ouverts s'il l'était déjà. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!valider {Critère}` : Change le type du rapport en Rapport si c'était une idée et fait le même effet que c!critiqué. Le Critère doit être assez fin pour aboutir à un unique écrit.", false);
				b.addField("Commandes de modification d'un écrit",  "`c!statut {Critère};{Statut}` : Change le statut de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!type {Critère};{Type}` : Change le type de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!auteur {Critère};{Auteur}` : Change l'auteur de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!ouvrir {Critère}` : Raccourci pour `c!statut {Critère};Ouvert`.\n"
						+ "`c!renommer {Critère};{Nouveau nom}` : Renomme un écrit.\n"
						+ "`c!ajouter_tag {Critère};{Tag}` : Ajoute un tag à l'écrit.\n"
						+ "`c!retirer_tag {Critère};{Critère tag}` : Retire un tag à l'écrit.", false);
				b.addField("Commandes d'entretien de la base de données (À utiliser avec précaution)",
						"`c!nettoyer` : Supprime tous les écrits abandonnés / refusés / publiés de la liste.\n"
						+ "`c!archiver_avant {Date}` : Met le statut « sans nouvelles » à tous les écrits n'ayant pas été mis à jour avant la date indiquée. La date doit être au format dd/mm/yyyy.\n"
						+ "`c!nettoyer_fort` : Supprime tous les écrits abandonnés / refusés / publiés / sans nouvelles de la liste.\n"
						+ "`c!doublons` : Supprime les éventuels doublons.", false);
				b.addField("Recherche avancée", "La recherche avancée est utilisable avec `c!ulister param;param;param;…`. Chaque paramètre est de la forme `nom=valeur`. Les différents paramètres disponibles sont :\n"
						+ "`nom={Critère}` : Réduit la recherche aux écrits dont le nom correspond au critère.\n"
						+ "`statut={Statut},{Statut},…` : Les écrits doivent avoir l'un des statuts de la liste.\n"
						+ "`type={Type},{Type},…` : Les écrits doivent avoir l'un des types de la liste.\n"
						+ "`auteur={Critère auteur},{Critère auteur},…` : Les écrits doivent être d'un des auteurs de la liste.\n"
						+ "`tag={Critère tag},{Critère tag},…` : Les écrits doivent posséder l'un des tags de la liste.\n"
						+ "`tag&={Critère tag},{Critère tag},…` : Les écrits doivent posséder tous les tags de la liste.\n"
						+ "`avant=jj/mm/aaaa` : Les écrits doivent avoir été modifiés pour la dernière fois avant la date indiquée.\n"
						+ "`après=jj/mm/aaaa` : Les écirts doivent avoir été modifiés pour la dernière fois après la date indiquée.\n", false);
				b.addField("Code source", "Disponible sur [Github](https://github.com/cyriellecentori/critibot).", false);
				b.setFooter("Version 2.3");
				b.setAuthor("Critibot", null, "https://media.discordapp.net/attachments/719194758093733988/842082066589679676/Critiqueurs5.jpg");
				message.getChannel().sendMessage(b.build()).queue();
			}
		});
		
		commands.put("ajouter", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					ecrits.add(new Ecrit(args[0],  args[4], Type.getType(args[2]), Status.getStatus(args[3]), args[1]));
					message.getChannel().sendMessage("Ajouté !").queue();
				} catch (ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!add Nom;Auteur;Type;Statut;URL.").queue();
				}
			}
			
		});
		
		commands.put("lister", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					// Les énumérations restent « null » si tout est accepté.
					Status status = (args[0].equalsIgnoreCase("tout") ? null : Status.getStatus(args[0]));
					Type type = null;
					if(args.length > 1) // Gestion du paramètre optionnel de type.
						type = (args[1].equalsIgnoreCase("tout") ? null : Type.getType(args[1]));
					
					// Vector des messages à envoyer.
					Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
					// Vector du contenu des embeds.
					Vector<String> messages = new Vector<String>();
					// Sépare les résultats de la recherche pour que chaque message n'excède pas les 2 000 caractères.
					String buffer = "";
					try {
					for(Ecrit e : sortByDate(ecrits)) { // Recherche tous les écrits respectant les critères demandés et les ajoute aux résultats.
						if(e.complyWith(type, status)) {
							// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans « messages » et le vide.
							String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " — " + e.getType() + "\n\n";
							if(buffer.length() + toAdd.length() > 2000) {
								messages.add(buffer);
								buffer = "";
							}
							buffer += toAdd;
						}
							
					}
					} catch(IndexOutOfBoundsException e) {
						e.printStackTrace();
					}
					if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais été rempli : aucun résultat, donc.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun résultat");
						b.setAuthor("Recherche : " + ((status == null) ? "Tous statuts" : status) + " — " + ((type == null) ? "Tous types" : type));
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						message.getChannel().sendMessage(b.build()).queue();
					} else { // Sinon, envoie les résultats.
						messages.add(buffer); // Ajoute le buffer restant aux messages.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Résultats de la recherche");
						b.setAuthor("Recherche : " + ((status == null) ? "Tous statuts" : status) + " — " + ((type == null) ? "Tous types" : type));
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) { // Crée les différents messages à envoyer en numérotant les pages.
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());
						}
						
						for(MessageEmbed embed : embeds) { // Envoie les messages.
							message.getChannel().sendMessage(embed).queue();
						}
					}
					
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!lister Statut;Type. Il est possible de ne pas spécifier le type si non nécessaire, alors la commande sera c!list Statut et tous les types seront affichés.").queue();
				}
			}
			
		});
		commands.put("l", new BotCommand.Alias(commands.get("lister")));
		
		commands.put("plop", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("plop").queue();
				
			}
			
		});
		
		commands.put("rechercher", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					Vector<Ecrit> res = bot.searchEcrit(args[0]); // Récupère tous les résultats de la recherche.
					if(res.isEmpty()) { // S'il n'y a aucun résultat.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun résultat");
						b.setAuthor("Recherche : " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						message.getChannel().sendMessage(b.build()).queue();
					} else if(res.size() <= 3) { // Si trois résultats ou moins, afficher les trois écrits en grand.
						for(Ecrit e : res) {
							message.getChannel().sendMessage(e.toEmbed()).queue();
						}
					} else { // Sinon, afficher une liste similaire à celle de c!lister. Voir cette commande pour plus de commentaires.
						Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
						Vector<String> messages = new Vector<String>();
						String buffer = "";
						for(Ecrit e : res) {
							String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " — " + e.getType() + "\n\n";
							if(buffer.length() + toAdd.length() > 2000) {
								messages.add(buffer);
								buffer = "";
							}
							buffer += toAdd;
						}
						messages.add(buffer);
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Résultats de la recherche");
						b.setAuthor("Recherche : " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) {
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());

							for(MessageEmbed embed : embeds) {
								message.getChannel().sendMessage(embed).queue();
							}
						}
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Rechercher… Quoi exactement ?").queue();
				}
			}

		});
		
		commands.put("s", new BotCommand.Alias(commands.get("rechercher")));
		
		commands.put("marquer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.marquer(message.getMember()))
					message.getChannel().sendMessage("« " + e.getNom() + " » marqué par " + message.getMember().getEffectiveName() + " !").queue();
				else if(e.getStatus() != Status.OUVERT)
					message.getChannel().sendMessage("« " + e.getNom() + " » ne peut avoir une marque d'intérêt car il n'est pas ouvert.").queue();
				else
					message.getChannel().sendMessage("Vous avez déjà marqué votre intérêt pour « " + e.getNom() + " ».").queue();
			}
		});

		
		commands.put("m", new BotCommand.Alias(commands.get("marquer")));
				
		commands.put("libérer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					message.getChannel().sendMessage("« " + e.getNom() + " » n'a aucune marque d'intérêt.").queue();
					return;
				}
				if(e.liberer(message.getMember())) {
					message.getChannel().sendMessage("Marque d'intérêt sur « " + e.getNom() + " » supprimée.").queue();
				} else {
					message.getChannel().sendMessage("Vous n'avez pas de marque d'intérêt sur « " + e.getNom() + " ».").queue();
				}
			}
		});
		
		commands.put("nettoyer", new BotCommand() {
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean(false);
				message.getChannel().sendMessage("Écrits abandonnés, publiés et refusés supprimés de la liste !").queue();
			}
		});
		
		commands.put("statut", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Status status = Status.getStatus(args[1]);
				boolean ret = e.setStatus(status);
				if(!ret) {
					message.getChannel().sendMessage("Ce statut ne peut être assigné manuellement.").queue();
				} else
					message.getChannel().sendMessage("Statut de « " + e.getNom() + " » changé !").queue();
			}
		});
		
		commands.put("status", new BotCommand.Alias(commands.get("statut")));
		
		commands.put("type", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Type status = Type.getType(args[1]);
				e.setType(status);
				message.getChannel().sendMessage("Type de « " + e.getNom() + " » changé !").queue();
			}
		});
		
		commands.put("valider", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				e.promote();
				message.getChannel().sendMessage("Si l'écrit « " + e.getNom() + " » était une idée, c'est maintenant un rapport ! Sinon, rien n'a changé.").queue();
				e.setStatus(Status.EN_ATTENTE);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été noté comme critiqué !").queue();
				
			}
		});
		
		commands.put("refuser", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				e.setStatus(Status.REFUSE);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été refusé.").queue();
				
			}
		});
		
		commands.put("refusé", new BotCommand.Alias(commands.get("refuser")));
		
		commands.put("supprimer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.remove(e);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été supprimé.").queue();	
			}
		});
		
		commands.put("marquer_pour", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length < 2) {
					message.getChannel().sendMessage("Il manque un argument. Utilisation : c!marquer_pour {Critère};{Utilisateur}").queue();
					return;
				}
				archiver();
				if(e.marquer(args[1])) {
					message.getChannel().sendMessage("« " + e.getNom() + " » marqué d'intêret pour " + args[1] + " !").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être marqué d'intérêt car il n'est pas ouvert.").queue();
				}
				
			}
		});
		
		commands.put("libérer_pour", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length < 2) {
					message.getChannel().sendMessage("Il manque un argument. Utilisation : c!libérer_pour {Critère};{Utilisateur}").queue();
					return;
				}
				archiver();
				if(e.getStatus() != Status.OUVERT_PLUS) {
					message.getChannel().sendMessage("« " + e.getNom() + " » n'a aucune marque d'intérêt.").queue();
					return;
				}
				if(e.liberer(args[1])) {
					message.getChannel().sendMessage("Marque d'intérêt de " + args[1] + " sur « " + e.getNom() + " » supprimée.").queue();
				} else {
					message.getChannel().sendMessage(args[1] + " n'a pas de marque d'intérêt sur « " + e.getNom() + " ».").queue();
				}
			}
		});
		
		commands.put("liberer_pour", new BotCommand.Alias(commands.get("libérer_pour")));
		
		commands.put("critiqué", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				message.getChannel().sendMessage("« " + e.getNom() + " » critiqué !").queue();
				e.critique();
			}
		});
		
		commands.put("critique", new BotCommand.Alias(commands.get("critiqué")));
		
		commands.put("inbox", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				String mes = "Boîte de réception :\n";
				for(SyndEntry e : bot.getInbox()) {
					mes += e.getTitle() + " — " + e.getLink() + "\n";
				}
				sendLongMessage(mes, message.getChannel());
				bot.getInbox().clear();
				bot.getJda().getPresence().setStatus(OnlineStatus.ONLINE);
				
			}
		});
		
		commands.put("archiver_avant", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				long date = 0L;
				try {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
					int n = 0;
					for(Ecrit e : bot.getEcrits()) {
						if(e.olderThan(date) && !e.isDead()) {
							e.setStatus(Status.SANS_NOUVELLES);
							n++;
						}
					}
					message.getChannel().sendMessage(n + " écrits ont été marqués sans nouvelles depuis le " + new SimpleDateFormat("dd MMM yyyy").format(new Date(date)) + ".").queue();
				} catch(ParseException | NullPointerException e) {
					message.getChannel().sendMessage("Erreur dans le format de la date.").queue();
				}
				
			}
			
		});
		
		commands.put("annuler", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(!bot.annuler()) {
					message.getChannel().sendMessage("Aucune modification effectuée.").queue();
				} else {
					message.getChannel().sendMessage("Dernière modification annulée !").queue();
				}
				
			}
			
		});
		
		commands.put("update_open", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.updateOpen();
				
			}
			
		});
		
		commands.put("refresh_messages", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.refreshMessages();
				
			}
			
		});
		
		commands.put("maj", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					int tailleAncienne = bot.getEcrits().size();
					addNew();
					if(tailleAncienne != bot.getEcrits().size()) {
						bot.updateOpen();
					}
					message.getChannel().sendMessage("Mise à jour effectuée.").queue();
				} catch (IllegalArgumentException | FeedException | IOException | NullPointerException e) {
					jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getLocalizedMessage()).queue();
					e.printStackTrace();
				}
				
			}
			
		});
		
		commands.put("renommer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				String oldName = e.getNom();
				try {
					e.rename(args[1]);
					message.getChannel().sendMessage("« " + oldName + " » renommé en « " + e.getNom() + " ».").queue();
				} catch(ArrayIndexOutOfBoundsException ex) {
					message.getChannel().sendMessage("Utilisation : `c!rename {Critère};{Nouveau nom}`").queue();
				}
			}
		});
		
		commands.put("doublons", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.doublons();
				message.getChannel().sendMessage("Doublons supprimés !").queue();
			}
			
		});
		
		commands.put("nettoyer_fort", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean(true);
				message.getChannel().sendMessage("Écrits abandonnés, refusés, publiés et sans nouvelles supprimés de la liste !").queue();
			}
			
		});
		
		commands.put("up", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				for(Affichan aff : affichans) {
					aff.up(e);
				}
				if(e.getStatus() != Status.RESERVE) {
					e.setStatus(Status.OUVERT);
				}
				message.getChannel().sendMessage("« " + e.getNom() + " » up !").queue();
			}
		});
		
		commands.put("réouvert", new BotCommand.Alias(commands.get("up")));
		commands.put("ouvrir", new BotCommand.Alias(commands.get("up")));
		commands.put("o", new BotCommand.Alias(commands.get("up")));
		commands.put("reouvert", new BotCommand.Alias(commands.get("up")));
		
		commands.put("manual_update", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.lastUpdate = System.currentTimeMillis();
				bot.getJda().getPresence().setActivity(Activity.playing("critiquer. Dernière mise à jour forum le " + new SimpleDateFormat("dd MMM yyyy à HH:mm").format(new Date(lastUpdate))));
				message.getChannel().sendMessage("Bot indiqué comme mis à jour.").queue();
				
			}
			
		});
		
		commands.put("bdd", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					bot.save();
					message.getChannel().sendFile(new File("critibot.json")).queue();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					message.getChannel().sendMessage("Impossible de sauvegarder la base de données en premier lieu.").queue();
				}
				
				
			}
			
		});
		
		commands.put("auteur", new BotCommand.SearchCommand() {

			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					e.setAuteur(args[1]);
					message.getChannel().sendMessage("« " + args[1] + " » a été défini comme l'auteur(ice) de « " + e.getNom() + " ».").queue();
				} catch(ArrayIndexOutOfBoundsException ex) {
					message.getChannel().sendMessage("Utilisation : `c!auteur {Critère};{Auteur}`").queue();
				}
				
			}
			
		});
		
		commands.put("taille_bdd", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("Il y a actuellement " + bot.getEcrits().size() + " écrits dans la base de données.").queue();
			}
			
		});
		
		commands.put("ulister", new BotCommand() {
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				// Liste de tous les paramètres possibles
				String critere = "";
				Vector<Status> status = new Vector<Status>();
				Vector<Type> type = new Vector<Type>();
				Vector<String> authors = new Vector<String>();
				Vector<String> tags = new Vector<String>();
				boolean tagAnd = false;
				long modAvant = 0;
				long modApres = 0;
				
				// Récupération des paramètres
				for(int i = 0; i < args.length; i++) {
					String[] c = args[i].split("=");
					if(c.length != 2) {
						message.getChannel().sendMessage("Erreur de syntaxe dans le paramètre " + (i+1) + " : le paramètre n'est pas séparables en deux parties par un symbole « = ».").queue();
						return;
					}
					String npara = basicize(c[0]);
					if(npara.equals("critere") || npara.equals("nom")) {
						critere = c[1];
					} else if(npara.startsWith("statut") || npara.equals("status")) {
						String[] st = c[1].split(",");
						for(String s : st) {
							status.add(Status.getStatus(s));
						}
						
					} else if(npara.startsWith("type")) {
						String[] ty = c[1].split(",");
						for(String t : ty) {
							type.add(Type.getType(t));
						}
					} else if(npara.startsWith("auteur") || npara.startsWith("autrice")) {
						String[] aut = c[1].split(",");
						String errMes = "";
						boolean quit = false;
						for(String au : aut) {
							Vector<String> found = autSearch(au);
							if(!found.isEmpty())
								for(String a : found) {
									authors.add(a);
								}
							else
								errMes += "Auteur « " + au + " » non trouvé.\n";
						}
						if(aut.length != 0 && authors.isEmpty()) {
							errMes += "Aucun auteur de la liste trouvé ; annulation.";
						}
						if(errMes != "") {
							message.getChannel().sendMessage(errMes).queue();
							return;
						}
					} else if(npara.startsWith("tag")) {
						String[] ta = c[1].split(",");
						for(String t : ta) {
							tags.add(t);
						}
						tagAnd = npara.contains("&");
					} else if(npara.equals("avant")) {
						try {
							modAvant = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
						} catch (ParseException e) {
							message.getChannel().sendMessage("Erreur de syntaxe dans le paramètre " + (i+1) + " : la date doit être au format jj/mm/aaaa.").queue();
							return;
						}
					} else if(npara.equals("apres")) {
						try {
							modApres = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
						} catch (ParseException e) {
							message.getChannel().sendMessage("Erreur de syntaxe dans le paramètre " + (i+1) + " : la date doit être au format jj/mm/aaaa.").queue();
							return;
						}
					} else {
						message.getChannel().sendMessage("Erreur de syntaxe dans le paramètre " + (i+1) + " : paramètre « " + c[0] + " » inconnu.").queue();
						return;
					}
				}
				
				Vector<Ecrit> candidats = new Vector<Ecrit>();
				if(critere.isEmpty()) {
					candidats = ecrits;
				} else {
					candidats = searchEcrit(critere);
				}
				Vector<Ecrit> choisis = new Vector<Ecrit>();
				for(Ecrit e : candidats) {
					boolean ok = true;
					if(!status.isEmpty())
						ok = ok && status.contains(e.getStatus());
					if(!type.isEmpty())
						ok = ok && type.contains(e.getType());
					if(!authors.isEmpty())
						ok = ok && authors.contains(e.getAuteur());
					if(!tags.isEmpty()) {
						boolean okTag = tagAnd;
						for(String tag : tags) {
							if(tagAnd) {
								okTag = okTag && e.hasTag(tag);
							} else {
								okTag = okTag || e.hasTag(tag);
							}
						}
						ok = ok && okTag;
					}
					if(modAvant != 0) {
						ok = ok && (e.getLastUpdateLong() < modAvant);
					}
					if(modApres != 0) {
						ok = ok && (e.getLastUpdateLong() > modApres);
					}
					if(ok) {
						choisis.add(e);
					}
				}
				
				// Vector des messages à envoyer.
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				// Vector du contenu des embeds.
				Vector<String> messages = new Vector<String>();
				// Sépare les résultats de la recherche pour que chaque message n'excède pas les 2 000 caractères.
				String buffer = "";
				for(Ecrit e : sortByDate(choisis)) {
					// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans « messages » et le vide.
					String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " — " + e.getType() + "\n\n";
					if(buffer.length() + toAdd.length() > 2000) {
						messages.add(buffer);
						buffer = "";
					}
					buffer += toAdd;
				}
				if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais été rempli : aucun résultat, donc.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun résultat");
					b.setAuthor("Recherche personnalisée");
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					message.getChannel().sendMessage(b.build()).queue();
				} else { // Sinon, envoie les résultats.
					messages.add(buffer); // Ajoute le buffer restant aux messages.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Résultats de la recherche");
					b.setAuthor("Recherche personnalisée");
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) { // Crée les différents messages à envoyer en numérotant les pages.
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}
					
					for(MessageEmbed embed : embeds) { // Envoie les messages.
						message.getChannel().sendMessage(embed).queue();
					}
				}
			}
		});
		
		commands.put("ul", new BotCommand.Alias(commands.get("ulister")));
		
		commands.put("ajouter_tag", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.addTag(args[1])) {
					message.getChannel().sendMessage("Tag « " + args[1] + " » ajouté à l'écrit « " + e.getNom() + " ».").queue();
				} else {
					message.getChannel().sendMessage("Ce tag est déjà ajouté à l'écrit « " + e.getNom() + " ».").queue();
				}
			}
		});
		
		commands.put("atag", new BotCommand.Alias(commands.get("ajouter_tag")));
		
		commands.put("retirer_tag", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.removeTag(args[1])) {
					message.getChannel().sendMessage("Tag correspondant au critère retiré de l'écrit « " + e.getNom() + " ».").queue();
				} else {
					message.getChannel().sendMessage("Aucun tag correspondant au critère n'est assigné à l'écrit « " + e.getNom() + " », ou plus d'un tag de l'écrit correspondent au critère.").queue();
				}
				
			}
		});
		
		commands.put("supprimer_tag", new BotCommand.Alias(commands.get("retirer_tag")));
		commands.put("rtag", new BotCommand.Alias(commands.get("retirer_tag")));
		commands.put("stag", new BotCommand.Alias(commands.get("retirer_tag")));
		
		commands.put("lister_tags", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				Vector<String> tags = new Vector<String>();
				Vector<Integer> nbEcrits = new Vector<Integer>();
				for(Ecrit e : bot.ecrits) {
					for(String tag : e.getTags()) {
						int index = tags.indexOf(tag);
						if(index < 0) {
							index = tags.size();
							tags.add(tag);
							nbEcrits.add(0);
						}
						nbEcrits.set(index, nbEcrits.get(index) + 1);
					}
				}
				
				// Vector des messages à envoyer.
				Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
				// Vector du contenu des embeds.
				Vector<String> messages = new Vector<String>();
				// Sépare les résultats de la recherche pour que chaque message n'excède pas les 2 000 caractères.
				String buffer = "";
				for(int i = 0; i < tags.size(); i++) {
					// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans « messages » et le vide.
					String toAdd = "**" + tags.get(i) + "**\n" + nbEcrits.get(i).toString() + " écrits\n\n";
					if(buffer.length() + toAdd.length() > 2000) {
						messages.add(buffer);
						buffer = "";
					}
					buffer += toAdd;
				}
				if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais été rempli : aucun résultat, donc.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Aucun tag dans la base de données");
					b.setAuthor("Liste des tags");
					b.setTimestamp(Instant.now());
					b.setColor(16001600);
					message.getChannel().sendMessage(b.build()).queue();
				} else { // Sinon, envoie les résultats.
					messages.add(buffer); // Ajoute le buffer restant aux messages.
					EmbedBuilder b = new EmbedBuilder();
					b.setTitle("Liste des tags");
					b.setAuthor("Liste des tags");
					b.setTimestamp(Instant.now());
					b.setColor(73887);
					for(int i = 0; i < messages.size(); i++) { // Crée les différents messages à envoyer en numérotant les pages.
						b.setFooter("Page " + (i + 1) + "/" + messages.size());
						b.setDescription(messages.get(i));
						embeds.add(b.build());
					}
					
					for(MessageEmbed embed : embeds) { // Envoie les messages.
						message.getChannel().sendMessage(embed).queue();
					}
				}
				
			}
			
		});
		
		commands.put("hash", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length > 0)
					message.getChannel().sendMessage("" + args[0].hashCode()).queue();
				
			}
			
		});
		
		commands.put("updates_from", new BotCommand() {
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(args.length == 0) {
					message.getChannel().sendMessage("Utilisation : c!updates_from jj/mm/aaaa").queue();
					return;
				}
				long date = 0L;
				try {
					date = new SimpleDateFormat("dd/MM/yyyy").parse(args[0]).getTime();
				} catch (ParseException e) {
					message.getChannel().sendMessage("Utilisation : c!updates_from jj/mm/aaaa").queue();
					return;
				}
				try {
					Vector<Ecrit> ecritsMaj = new Vector<Ecrit>();
					Vector<String> neo = new Vector<String>();
					SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/cp-656675.xml")));
					for(Object o : feed.getEntries()) {
						SyndEntry entry = (SyndEntry) o;
						if(entry.getPublishedDate().after(new Date(date))) {
							String threadID = entry.getLink().substring(7).split("/")[2];
							Ecrit ecr = null;
							for(Ecrit e : ecrits) {
								String eID = e.getLien().substring(7).split("/")[2];
								if(threadID.equals(eID)) {
									ecr = e;
									break;
								}
							}
							if(ecr != null && !ecritsMaj.contains(ecr)) {
								ecritsMaj.add(ecr);
							} else if(!neo.contains(entry.getLink()) && ecr == null) {
								neo.add(entry.getLink());
							}
						}
					}
					// Vector des messages à envoyer.
					Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
					// Vector du contenu des embeds.
					Vector<String> messages = new Vector<String>();
					// Sépare les résultats de la recherche pour que chaque message n'excède pas les 2 000 caractères.
					String buffer = "";
					for(Ecrit e : sortByDate(ecritsMaj)) {
						// Remplit d'abord un buffer puis lorsqu'il est trop grand, ajoute son contenu dans « messages » et le vide.
						String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getAuteur() + "\n" + e.getStatus() + " — " + e.getType() + "\n\n";
						if(buffer.length() + toAdd.length() > 2000) {
							messages.add(buffer);
							buffer = "";
						}
						buffer += toAdd;
					}
					
					for(String str : neo) {
						String toAdd = str + "\n\n";
						if(buffer.length() + toAdd.length() > 2000) {
							messages.add(buffer);
							buffer = "";
						}
						buffer += toAdd;
					}
					if(buffer.isEmpty()) { // Si le buffer est vide, c'est qu'il n'a jamais été rempli : aucun résultat, donc.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Aucun résultat");
						b.setAuthor("Écrits mis à jour depuis le " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(16001600);
						message.getChannel().sendMessage(b.build()).queue();
					} else { // Sinon, envoie les résultats.
						messages.add(buffer); // Ajoute le buffer restant aux messages.
						EmbedBuilder b = new EmbedBuilder();
						b.setTitle("Résultats de la recherche");
						b.setAuthor("Écrits mis à jour depuis le " + args[0]);
						b.setTimestamp(Instant.now());
						b.setColor(73887);
						for(int i = 0; i < messages.size(); i++) { // Crée les différents messages à envoyer en numérotant les pages.
							b.setFooter("Page " + (i + 1) + "/" + messages.size());
							b.setDescription(messages.get(i));
							embeds.add(b.build());
						}
						
						for(MessageEmbed embed : embeds) { // Envoie les messages.
							message.getChannel().sendMessage(embed).queue();
						}
					}
				} catch (IllegalArgumentException | FeedException | IOException e) {
					message.getChannel().sendMessage("Erreur lors de la récupération du flux.").queue();
				}
				
			}
		});
	}
	
	/**
	 * Supprime les doublons de la liste.
	 */
	public void doublons() {
		// Liste des noms des écrits déjà présents dans la BDD.
		Vector<String> noms = new Vector<String>();
		// Liste des écrits en trop à supprimer.
		Vector<Ecrit> toDel = new Vector<Ecrit>();
		for(Ecrit e : ecrits) {
			if(noms.contains(basicize(e.getNom()))) { // Si le nom existe déjà : poubelle
				toDel.add(e);
			} else {
				noms.add(basicize(e.getNom())); // Sinon on enrigistre le nom.
			}
		}
		for(Ecrit e : toDel) { // Suppression des doublons.
			ecrits.remove(e);
		}
	}
	
	/**
	 * Annule la dernière action et revient à la dernière sauvegarde.
	 */
	public boolean annuler() {
		if(cancel.empty()) { // Si aucune sauvegarde, pas de bol, tant pis
			return false;
		} else {
			ecrits = cancel.pop(); // Sinon on la récupère en la sortant du tas et on remplace la BDD actuelle
			for(Affichan aff : affichans) { // Change les références d'écrits dans tous les affichans
				aff.updateRefs(ecrits);
			}
			return true;
		}
	}
	
	public Vector<Ecrit> getEcrits() {
		return ecrits;
	}
	
	public BotCommand getCommand(String name) {
		return commands.get(name);
	}
	
	public JDA getJda() {
		return jda;
	}
	
	public Vector<SyndEntry> getInbox() {
		return inbox;
	}
	
	/**
	 * Archive la BDD actuelle dans la pile d'annulation.
	 */
	public void archiver() {
		// Effectue une copie profonde du vecteur, sinon les écrits modifiés dans la BDD le seront aussi dans les sauvegardes.
		cancel.add(new Vector<Ecrit>());
		for(Ecrit e : ecrits) {
			cancel.peek().add(e.clone());
		}
		if(cancel.size() > 20) { // Supprime les anciennes sauvegardes pour éviter une surcharge de la mémoire.
			cancel.remove(0);
		}
	}
	
	/**
	 * Sheduler qui execute la recherche de flux RSS toutes les 10mn.
	 */
	private ScheduledExecutorService shreduler = Executors.newScheduledThreadPool(1);

	@Override
	public void onEvent(GenericEvent event) {
		if(event instanceof ReadyEvent) { // Lorsque le bot est prêt
			// Intitialise le shreduler pour les vérifications régulières de nouveaux fils.
			shreduler.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					try {
						int tailleAncienne = ecrits.size();
						addNew(); // Vérifie les nouveaux écrits
						if(tailleAncienne != ecrits.size()) { // S'il y a des nouveaux ajouts, mettre à jour les messages (sinon c'est pas la peine).
							updateOpen();
						}
					} catch (IllegalArgumentException | FeedException | IOException e) {
						e.printStackTrace();
						jda.getTextChannelById(737725144390172714L).sendMessage("<@340877529973784586>\n" + e.getClass().getCanonicalName() + "\n" + e.getLocalizedMessage()).queue();
					}
					System.out.println("Shreduled update.");
				}
				
			}, 10, 10, TimeUnit.MINUTES);
			jda.getPresence().setActivity(Activity.playing("critiquer. Aucune mise à jour forum depuis le redémarrage."));
			for(Affichan aff : affichans) {
				try {
					aff.initialize(this);
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}

		}
		if(event instanceof MessageDeleteEvent) {
			for(Affichan aff : affichans) {
				aff.checkDeletion(this, (MessageDeleteEvent) event);
			}
		} else if(event instanceof MessageReactionAddEvent) { // Traitement des réactions
			MessageReactionAddEvent mrae = (MessageReactionAddEvent) event;
			if(!mrae.getUser().isBot())
				for(Affichan aff : affichans)
					if(mrae.getChannel().getIdLong() == aff.chanID)
						aff.reactionAdd(this, mrae);
			
		} else if(event instanceof MessageReceivedEvent) { // Message reçu
			MessageReceivedEvent mre = (MessageReceivedEvent) event;
			
			// Termine directement si ce n'est pas une commande ou un message envoyé par un bot.
			if(!mre.getMessage().getContentRaw().startsWith(prefix + "!") || mre.getAuthor().isBot() || mre.getAuthor().getId().equals(jda.getSelfUser().getId()))
				return;
			
			// Récupère la commande
			String command = mre.getMessage().getContentRaw().split(" ", 2)[0].split("!", 2)[1];
			
			// Récupère les arguments
			String args;
			try {
				args = mre.getMessage().getContentRaw().split(" ", 2)[1];
			} catch(ArrayIndexOutOfBoundsException e) {
				args = "";
			}
			// Log la commande
			System.out.println(command);
			System.out.println(args);
			System.out.println("——————————");
			
			MessageChannel chan = mre.getChannel();
			
			// Essaye d'executer la commande demandée
			try {
				commands.get(command).execute(this, mre, args.split(";"));
			} catch(NullPointerException e) { // Si elle n'est pas trouvée, c'est qu'elle est inconnue.
				chan.sendMessage("Commande inconnue.").queue();
			}
			
			//Met à jour les messages et sauvegarde après chaque commande.
			updateOpen();
			try {
				save();
			} catch (IOException e) {
				if(!errorSave) {
					mre.getChannel().sendMessage("Impossible de sauvegarder les données. Je vais continuer à essayer, mais n'enverrai pas d'autres messages si cela échoue.").queue();
					e.printStackTrace();
					errorSave = true;
				}
			}
		}
		
	}

}
