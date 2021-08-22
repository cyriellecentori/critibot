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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
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
	private JDA jda;
	
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
	/**
	 * Le salon « organisation » où seront placés les réservations.
	 */
	private final long organichan;
	/**
	 * Le salon « fils ouverts » où seront affichés les écrits ouverts à la critique.
	 */
	private final long openchan;
	/**
	 * L'emote de réservation.
	 */
	private final long henritueur;
	/**
	 * L'emote indiquant l'écrit comme critiqué.
	 */
	private final long henricheck;
	/**
	 * L'emote indiquant l'écrit comme refusé.
	 */
	private final long henricross;
	//private final String whiteCheckBox = "U+2705";
	/**
	 * L'emoji cadenas ouvert.
	 */
	private final String unlock = "U+1f513";
	//private final String cross = "U+274e";
	/**
	 * Le préfixe du bot.
	 */
	private final String prefix;
	/**
	 * La date de la dernière mise à jour manuelle du bot.
	 */
	public long lastUpdate;
	
	public CritiBot(String token) {
		this.token = token;
		if(token.hashCode() == 1973164890) { // Si le bot est connecté à l'utilisateur Critibot#8684, les salons sont ceux du discord des Critiqueurs.
			prefix = "c";
			organichan = 614947463610236939L;
			henritueur = 817064076244418610L;
			openchan = 843956373103968308L;
			henricheck = 843965097428516864L;
			henricross = 843965099986780200L;
		} else { // Sinon, le bot est considéré comme étant en bêta et les salons sont ceux de BrainBot's lair.
			prefix = "bc";
			organichan = 878917114474410004L;
			henritueur = 470138432723877888L;
			openchan = 878917114474410004L;
			henricheck = 470138433185120256L;
			henricross = 587611157158952971L;
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
						Vector<Ecrit> es = search("sans nom");
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
	private String basicize(String s) {
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
	
	/**
	 * Lance une recherche parmi les écrits. Le critère doit être consituée de parties de mots du titre, majuscules et diacritiques ignorées.
	 * @param s Le critère de recherche.
	 * @return Une liste d'écrits correspondant au critère.
	 */
	public Vector<Ecrit> search(String s) {
		Vector<Ecrit> list = new Vector<Ecrit>();
		// Sépare les mots du critère.
		String[] motsSearch = basicize(s).split(" ");
		for(Ecrit e : ecrits) { // Vérifie si chaque écrit respecte le critère.
			String[] mots = basicize(e.getNom()).split(" "); // Simplifie le titre de l'écrit et sépare ses mots.
			boolean ok = true;
			for(String motSearch : motsSearch) { // Vérifie si un mot de l'écrit contient un mot du critère
				boolean found = false;
				for(String mot : mots) { 
					if(mot.toLowerCase().contains(motSearch.toLowerCase())) {
						found = true;
					}
				}
				if(!found) { // Si un mot du critère n'est pas présent dans un mot de l'écrit, alors l'écrit ne correspond pas au critère.
					ok = false;
					break;
				}
			}
			if(ok) {
				list.add(e);
			}
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
		// Sépare les mots du critère.
		String[] motsSearch = basicize(s).split(" ");
		
		// Récupère la liste de tous les auteurs de la base de données.
		Vector<String> auteurList = new Vector<String>();
		for(Ecrit e : ecrits) {
			if(!auteurList.contains(e.getAuteur()))
				auteurList.add(e.getAuteur());
		}
		
		// Recherche les auteurs de la même manière que la recherche d'écrits : se référer à celle-ci pour plus de commentaires.
		for(String a : auteurList) {
			String[] mots = basicize(a).split(" ");
			boolean ok = true;
			for(String motSearch : motsSearch) {
				boolean found = false;
				for(String mot : mots) {
					if(mot.toLowerCase().contains(motSearch.toLowerCase())) {
						found = true;
					}
				}
				if(!found) {
					ok = false;
					break;
				}
			}
			if(ok) {
				list.add(a);
			}
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
		for(Ecrit e : ecrits) {
			// Vérification des écrits ouverts : si l'écrit est ouvert mais n'a pas de message, il faut lui créer.
			if(e.getStatus() == Status.OUVERT && !e.getStatusMessage().isInitialized()) {
				Message m = jda.getTextChannelById(openchan).sendMessage(e.toEmbed()).complete();
				e.setStatusMessage(m);
				m.addReaction(jda.getEmoteById(henritueur)).queue();
				m.addReaction(jda.getEmoteById(henricheck)).queue();
				if(e.getType() == Type.IDEE)
					m.addReaction(jda.getEmoteById(henricross)).queue();
			} else if(e.getStatus() != Status.OUVERT && e.getStatusMessage().isInitialized()) { // Si l'écrit n'est pas ouvert et qu'il a un message dans le chan des écrits ouverts, il faut le supprimer.
				if(e.getStatusMessage().getMessage().getChannel().getIdLong() == openchan) {
					e.removeStatusMessage();
				}
			}
			
			// Vérification des écrits réservés de la même manière.
			if(e.getStatus() == Status.RESERVE && !e.getStatusMessage().isInitialized()) {
				Message m = jda.getTextChannelById(organichan).sendMessage(e.toEmbed()).complete();
				e.setStatusMessage(m);
				m.addReaction(jda.getEmoteById(henricheck)).queue();
				if(e.getType() == Type.IDEE)
					m.addReaction(jda.getEmoteById(henricross)).queue();
				m.addReaction(unlock).queue();
			} else if(e.getStatus() != Status.RESERVE && e.getStatusMessage().isInitialized())
				if(e.getStatusMessage().getMessage().getChannel().getIdLong() == organichan)
					e.removeStatusMessage();
		}
	}
	
	/**
	 * Supprime tous les messages des écrits. updateOpen() étant appelé à chaque commande, tous les messages seront recrées juste après.
	 */
	public void refreshMessages() {
		for(Ecrit e : ecrits) {
			if(e.getStatusMessage().isInitialized()) {
				e.removeStatusMessage();
			}
		}
	}
	
	/**
	 * Met à jour les messages des écrits en les modifiant tous.
	 */
	public void updateMessages() {
		for(Ecrit e : ecrits) {
			if(e.getStatusMessage().isInitialized()) {
				e.getStatusMessage().getMessage().editMessage(e.toEmbed()).queue();
			}
		}
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
				b.addField("Valeurs de Statut", "Statut doit être « Ouvert — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé — Infraction »", false);
				b.addField("Valeurs de Type", "Type doit être « Conte — Rapport — Idée — Autre »", false);
				b.addField("Commandes de base", "`c!aide` : Cette commande d'aide.\n"
						+ "`c!annuler` : Annule la dernière modification effectuée.", false);
				b.addField("Commandes de gestion et d'affichage de la liste", "`c!ajouter {Nom};{Type};{Statut};{URL}` : Ajoute manuellement un écrit à la liste.\n"
						+ "`c!supprimer {Critère}` : Supprime un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit. __**ATTENTION**__ : Il n'y a pas de confirmation, faites attention à ne pas vous tromper dans le Critère.\n"
						+ "`c!inbox` : Affiche la boîte de réception, contenant les écrits qui n'ont pas pu être ajoutés automatiquement. Attention, l'appel à cette commande supprime le contenu de la boîte.", false);
				b.addField("Commandes de recherche", "`c!rechercher {Critère}` : Affiche tous les écrits contenant {Critère}.\n"
						+ "`c!lister {Statut};[Type]` : Affiche la liste des écrits avec le statut et du type demandés. Statut et Type peuvent prendre la valeur « Tout ».\n"
						+ "`c!recherche_auteur {Critère de l'auteur};[Statut];[Type]` : Affiche la liste des écrits de l'auteur demandé, avec le statut et le type demandés si présents. Statut et Type peuvent prendre la valeur « Tout ». Le {Critère de l'auteur} fonctionne de la même manière que la recherche d'écrits. Il doit être également assez précis pour aboutir à un unique auteur.", false);
				b.addField("Commandes de critiques", "`c!réserver {Critère}` : Réserve un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réserver_pour {Critère};{Nom}` : Réserve un écrit pour quelqu'un d'autre. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!libérer {Critère}` : Supprime la réservation d'un écrit si vous êtes la personne l'ayant réservé, ou membre de l'équipe critique, ou que l'écrit a été réservé par procuration. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réservation {Critère}` : Affiche les informations de réservation d'un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!up {Critère}` : Marque un écrit ouvert et le remet au premier plan dans le salon des fils ouverts s'il l'était déjà. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!valider {Critère}` : Change le type du rapport en Rapport si c'était une idée et fait le même effet que c!critiqué. Le Critère doit être assez fin pour aboutir à un unique écrit.", false);
				b.addField("Commandes de modification d'un écrit",  "`c!statut {Critère};{Statut}` : Change le statut de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!type {Critère};{Type}` : Change le type de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!auteur {Critère};{Auteur}` : Change l'auteur de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!ouvrir {Critère}` : Raccourci pour `c!statut {Critère};Ouvert`.\n"
						+ "`c!renommer {Critère};{Nouveau nom}` : Renomme un écrit.", false);
				b.addField("Commandes d'entretien de la base de données (À utiliser avec précaution)",
						"`c!nettoyer` : Supprime tous les écrits abandonnés / refusés / publiés de la liste.\n"
						+ "`c!archiver_avant {Date}` : Met le statut « sans nouvelles » à tous les écrits n'ayant pas été mis à jour avant la date indiquée. La date doit être au format dd/mm/yyyy.\n"
						+ "`c!nettoyer_fort` : Supprime tous les écrits abandonnés / refusés / publiés / sans nouvelles de la liste.\n"
						+ "`c!doublons` : Supprime les éventuels doublons.", false);
				b.addField("Code source", "Disponible sur [Github](https://github.com/cyriellecentori/critibot).", false);
				b.setFooter("Version 1.5");
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
					for(Ecrit e : ecrits) { // Recherche tous les écrits respectant les critères demandés et les ajoute aux résultats.
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
					Vector<Ecrit> res = bot.search(args[0]); // Récupère tous les résultats de la recherche.
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
		
		commands.put("réserver", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue(); // Supprime la commande
				archiver();
				if(e.reserver(message.getMember())) { // Tente de réserver l'écrit
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé par " + message.getAuthor().getName() + " !").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
			}
		});
		
		commands.put("r", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("reserver", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("libérer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue(); // Supprime la commande
				archiver();
				if(e.getStatus() != Status.RESERVE) {
					message.getChannel().sendMessage("« " + e.getNom() + " » n'est pas réservé.").queue();
					return;
				}
				if(e.liberer(message.getMember())) {
					message.getChannel().sendMessage("Réservation sur « " + e.getNom() + " » supprimée.").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("Vous n'êtes pas à l'origine de la réservation sur « " + e.getNom() + " » ou vous n'êtes pas de l'équipe critique.").queue();
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
		
		commands.put("status", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				Status status = Status.getStatus(args[1]);
				boolean ret = e.setStatus(status);
				if(!ret) {
					if(status == Status.RESERVE) {
						message.getChannel().sendMessage("Utilisez c!réserver pour réserver un écrit.").queue();
					} else {
						message.getChannel().sendMessage("Cet écrit est réservé, libérez le d'abord avec c!libérer pour changer son statut.").queue();
					}
				} else
					message.getChannel().sendMessage("Statut de « " + e.getNom() + " » changé !").queue();
			}
		});
		
		commands.put("statut", new BotCommand.Alias(commands.get("status")));
		
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
				if(e.getStatus() == Status.RESERVE)
					if(!e.liberer(message.getMember()))
						message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » a déjà été réservé par quelqu'un d'autre. Je vais cependant vous croire sur le fait que vous avez critiqué l'écrit, mais vous avez probablement enfreint une réservation et c'est pas très gentil.").queue();
				e.setStatus(Status.EN_ATTENTE);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été noté comme critiqué !").queue();
				
			}
		});
		
		commands.put("réservation", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				if(e.getStatus() == Status.RESERVE)
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » est a été réservé par " + e.getReservation(message.getGuild()) + " le " + e.getResDate() + ".").queue();
				else
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » n'est pas réservé.").queue();
				
			}
		});
		
		commands.put("reservation", new BotCommand.Alias(commands.get("réservation")));
		
		commands.put("supprimer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.remove(e);
				message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a été supprimé.").queue();	
			}
		});
		
		commands.put("réserver_pour", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				if(e.reserver(args[1])) {
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé pour " + args[1] + " !").queue();
				} else {
					message.getAuthor().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
				
			}
		});
		
		commands.put("critiqué", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getMessage().delete().queue();
				archiver();
				message.getChannel().sendMessage("« " + e.getNom() + " » critiqué !").queue();
				if(!e.critique(message.getMember())) {
					message.getAuthor().openPrivateChannel().complete().sendMessage("Vous avez indiqué avoir critiqué « " + e.getNom() + " » mais vous n'étiez pas à l'origine de la réservation. Je vous fais confiance, mais sachez que c'est pas cool.").queue();
				}
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
						if(e.olderThan(date)) {
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
	
		commands.put("update_messages", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				bot.updateMessages();
				
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
				if(e.getStatusMessage().isInitialized()) {
					e.removeStatusMessage();
				} else if(e.getStatus() != Status.RESERVE) {
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
		
		commands.put("recherche_auteur", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					Vector<String> aut = bot.autSearch(args[0]); // Récupère tous les auteurs correspondants au critère.
					if(aut.isEmpty()) { // Si aucun auteur n'est dans la liste.
						message.getChannel().sendMessage("Aucun auteur correspondant trouvé.").queue();
					} else if(aut.size() > 1) { // Si trop d'auteurs sont dans la liste.
						message.getChannel().sendMessage("Plusieurs auteurs trouvés : affinez votre critère.").queue();
					} else { // Sinon, affiche tous leurs écrits.
						Vector<Ecrit> res = new Vector<Ecrit>();
						Ecrit.Status statut = null;
						if(args.length > 1) { // Paramètre optionnel du statut.
							if(args[1] != "Tout")
								statut = Status.getStatus(args[1]);
						}
						Type type = null;
						if(args.length > 2) { // Paramètre optionnel du type.
							if(args[2] != "Tout")
								type = Type.getType(args[2]);
						}
						
						for(Ecrit e : ecrits) { // Récupère les écrits qui
							boolean ok = (e.getAuteur().equals(aut.get(0))); // sont de cet auteur
							if(statut != null) { // sont du statut recherché si demandé
								ok = ok && (e.getStatus() == statut);
							}
							if(type != null) { // sont du type recherché si demandé
								ok = ok && (e.getType() == type);
							}
							if(ok) {
								res.add(e);
							}
						}
						if(res.isEmpty()) { // Si aucun écrit ne correspond à la recherche.
							EmbedBuilder b = new EmbedBuilder();
							b.setTitle("Aucun résultat");
							b.setAuthor("Recherche : " + args[0] + " – " + (statut == null ? "Tout" : statut) + " — " + (type == null ? "Tout" : type));
							b.setTimestamp(Instant.now());
							b.setColor(16001600);
							message.getChannel().sendMessage(b.build()).queue();
						} else { // Si des écrits sont trouvés, ils sont listés de la même manière qu'avec c!lister (voir cette commande pour plus de commentaires).
							Vector<MessageEmbed> embeds = new Vector<MessageEmbed>();
							Vector<String> messages = new Vector<String>();
							String buffer = "";
							for(Ecrit e : res) {
								String toAdd = "[**" + e.getNom() + "**](" + e.getLien() + ")\n" + e.getStatus() + " — " + e.getType() + "\n" + "Dernière modification : " + e.getLastUpdate() + "\n\n";
								if(buffer.length() + toAdd.length() > 2000) {
									messages.add(buffer);
									buffer = "";
								}
								buffer += toAdd;
							}
							messages.add(buffer);
							EmbedBuilder b = new EmbedBuilder();
							b.setTitle("Liste des écrits de " + aut.get(0));
							b.setAuthor("Recherche : " + args[0] + " – " + (statut == null ? "Tout" : statut) + " — " + (type == null ? "Tout" : type));
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
					}
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : `c!recherche_auteur {Critère de l'auteur};[Statut];[Type]`").queue();
				} catch(NullPointerException e) {
					e.printStackTrace();
					message.getChannel().sendMessage("Erreur.").queue();
				}
			}
			
		});
		
		commands.put("ra", new BotCommand.Alias(commands.get("recherche_auteur")));
		
		commands.put("taille_bdd", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				message.getChannel().sendMessage("Il y a actuellement " + bot.getEcrits().size() + " écrits dans la base de données.").queue();
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
	 * @return
	 */
	public boolean annuler() {
		if(cancel.empty()) { // Si aucune sauvegarde, pas de bol, tant pis
			return false;
		} else {
			ecrits = cancel.pop(); // Sinon on la récupère en la sortant du tas et on remplace la BDD actuelle
			for(Ecrit e : ecrits) { // Revérification de tous les messages des écrits
				e.getStatusMessage().retrieve(jda);
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
		if(event instanceof ReadyEvent) { // Lorsque le bot est prêt.
			for(Ecrit e : ecrits)
				e.check(jda); // Rechecks the messages
			
			// Initializes the scheduler
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

		}
		if(event instanceof MessageReactionAddEvent) { // Traitement des réactions
			MessageReactionAddEvent mrae = (MessageReactionAddEvent) event;
			if(!mrae.getUser().isBot()) // Ne traîte pas les réactions des bots, donc ses propres réactions
				for(Ecrit e : ecrits) { // Cherche l'écrit correspondant au message
					if(e.getStatusMessage().isInitialized())
						if(mrae.getMessageIdLong() == e.getStatusMessage().getMessage().getIdLong()) { // Si c'est vrai, on a enfin trouvé le bon écrit.
							if(mrae.getReactionEmote().isEmoji()) { // Vérifie les actions pour les emojis
								// Actions de libération de la réservation
								if(mrae.getReactionEmote().getAsCodepoints().equals(unlock) && e.getResId() == mrae.getUserIdLong() || (e.getResId() == 0L && e.getStatus() == Status.RESERVE)) {
									archiver();
									e.liberer(null);
									e.removeStatusMessage();
								}
							} else { // Vérifie les actions pour les emotes
								if(mrae.getReactionEmote().getEmote().getIdLong() == henritueur && mrae.getChannel().getIdLong() == openchan) { // Réservation
									if(e.getStatus() == Status.RESERVE && e.getResId() != mrae.getUserIdLong()) { // Essaye de réserver alors que ça l'est déjà
										mrae.getUser().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » est déjà réservé !").queue();
									} else {
										archiver();
										if(e.reserver(mrae.getMember())) { // Essaye de réserver l'écrit
											// Si réservé, envoie le message de réservation
											Message m = jda.getTextChannelById(organichan).sendMessage(e.toEmbed()).complete();
											e.removeStatusMessage();
											e.setStatusMessage(m);
											m.addReaction(jda.getEmoteById(henricheck)).queue();
											if(e.getType() == Type.IDEE)
												m.addReaction(jda.getEmoteById(henricross)).queue();
											m.addReaction(unlock).queue();
										} else { // Sinon (ça ne devrait pas arriver), indique à l'utilisateur que ce n'est pas possible.
											mrae.getUser().openPrivateChannel().complete().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un statut réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au statut inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
										}
									}
								} else if(mrae.getReactionEmote().getEmote().getIdLong() == henricross && e.getType() == Type.IDEE) { // Refus d'une idée
									archiver();
									e.liberer(null);
									e.setStatus(Status.REFUSE);
									jda.getTextChannelById(organichan).sendMessage("« " + e.getNom() + " » refusé !").queue();
									e.removeStatusMessage();
								} else if(mrae.getReactionEmote().getEmote().getIdLong() == henricheck) { // Idée indiquée comme critiquée
									archiver();
									jda.getTextChannelById(organichan).sendMessage("« " + e.getNom() + " » critiqué !").queue();
									if(!e.critique(mrae.getMember())) {
										mrae.getUser().openPrivateChannel().complete().sendMessage("Vous avez indiqué avoir critiqué « " + e.getNom() + " » mais vous n'étiez pas à l'origine de la réservation. Je vous fais confiance, mais sachez que c'est pas cool.").queue();
									}
									e.removeStatusMessage();
								} 
							}
							// Met à jour les messages
							updateOpen();
							try { // Essaye de sauvegarder
								save();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
				}
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
