package tk.cyriellecentori.CritiBot;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Stack;
import java.util.Vector;
import javax.security.auth.login.LoginException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import tk.cyriellecentori.CritiBot.Ecrit.Status;
import tk.cyriellecentori.CritiBot.Ecrit.Type;

public class CritiBot implements EventListener {
	
	public static void main(String[] args) {
		if(args.length < 1) {
			System.err.println("Merci d'indiquer le token du bot en paramètre.");
			return;
		}
		CritiBot cb = new CritiBot(args[0]);

	}
	
	private long lastCheck = 1620570510854L;
	private Vector<SyndEntry> inbox = new Vector<SyndEntry>();
	private String token;
	private JDABuilder builder;
	private JDA jda;
	private Gson gson;
	private Stack<Vector<Ecrit>> cancel = new Stack<Vector<Ecrit>>();
	private Vector<Ecrit> ecrits;
	private boolean errorSave = false;
	private LinkedHashMap<String, BotCommand> commands = new LinkedHashMap<String, BotCommand>();
	private long organichan = 614947463610236939L;
	
	public void addNew() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
		SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL("http://fondationscp.wikidot.com/feed/forum/ct-656675.xml")));
		
		for(Object e : feed.getEntries()) {
			SyndEntry entry = (SyndEntry) e;
			if(entry.getPublishedDate().after(new Date(lastCheck))) {
				if(entry.getTitle().contains("]")) {
					Type type = null;
					String[] sp = entry.getTitle().split("]");
					String balises = "";
					for(int i = 0; i < sp.length - 1; i++) {
						balises += sp[i] + "]";
					}
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
					if(unclean.isEmpty() || sp.length == 1) {
						Vector<Ecrit> es = search("sans nom");
						unclean = "(sans nom " + es.size() + ")";
						balises = entry.getTitle();
					}
					if(balises.contains("Idée") || balises.contains("idée")) {
						type = Type.IDEE;
					} else if(balises.contains("Conte") || balises.contains("Série")) {
						type = Type.CONTE;
					} else if(balises.contains("Refus")) {
						type = Type.AUTRE;
					} else {
						type = Type.RAPPORT;
					}
					
					ecrits.add(new Ecrit(unclean, entry.getLink(), type, Status.OUVERT));
					
				} else {
					inbox.add(entry);
					jda.getPresence().setStatus(OnlineStatus.IDLE);
				}
				
			}
		}
		lastCheck = System.currentTimeMillis();
	}
	
	public CritiBot(String token) {
		this.token = token;
		
		try {

			builder = JDABuilder.createDefault(this.token)
					.addEventListeners(this);

			jda = builder.build();
		} catch(LoginException | IllegalArgumentException e) {
			e.printStackTrace();
			return;
		}
		jda.setAutoReconnect(true);
		
		GsonBuilder gsonBilder = new GsonBuilder();
		gsonBilder.setPrettyPrinting();

		gson =gsonBilder.create();
		
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
		
		try {
			lastCheck = Long.parseLong(data.split("θ")[1]);
			ecrits = gson.fromJson(data.split("θ")[0], ttve.getType());
		} catch(Exception e) {
			ecrits = gson.fromJson(data, ttve.getType());
		}
		
		
		
		if(ecrits == null)
			ecrits = new Vector<Ecrit>();
		
		for(Ecrit e : ecrits)
			e.check();
		
		initCommands();
	}
	
	public void save() throws IOException {
		BufferedWriter dataFile = new BufferedWriter(new FileWriter("critibot.json"));
		dataFile.write(gson.toJson(ecrits) + "θ" + lastCheck);
		dataFile.close();
	}
	
	public Vector<Ecrit> search(String s) {
		Vector<Ecrit> list = new Vector<Ecrit>();
		for(Ecrit e : ecrits) {
			if(e.getNom().toLowerCase().contains(s.toLowerCase())) {
				list.add(e);
			}
		}
		return list;
	}
	
	public void clean() {
		for(Ecrit e : ecrits) {
			if(e.isDead())
				ecrits.removeElement(e);
		}
	}
	
	public void remove(Ecrit e) {
		ecrits.remove(e);
	}
	
	private void initCommands() {
		commands.put("aide", new BotCommand() {
			public void execute(CritiBot bb, MessageReceivedEvent message, String[] args) {
				BotCommand.sendLongMessage(""
						+ "Les paramètres entre crochets sont optionnels, entre accolades obligatoires.\n"
						+ "Possibilités pour les paramètres Status et Type :\n"
						+ "Status doit être « Ouvert — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé »..\n"
						+ "Type doit être « Conte — Rapport — Idée — Autre ».\n"
						+ "Liste des commandes :\n"
						+ "`c!aide` : Cette commande d'aide.\n"
						+ "`c!annuler` : Annule la dernière modification effectuée.\n"
						+ "__Commandes de gestion et d'affichage de la liste__ :\n"
						+ "`c!ajouter {Nom};{Type};{Status};{URL}` : Ajoute manuellement un écrit à la liste.\n"
						+ "`c!nettoyer` : Supprime tous les écrits abandonnés de la liste.\n"
						+ "`c!archiver_avant {Date}` : Met le status « sans nouvelles » à tous les écrits n'ayant pas été mis à jour avant la date indiquée. La date doit être au format dd/mm/yyyy.\n"
						+ "`c!rechercher {Critère}` : Affiche tous les écrits contenant {Critère}.\n"
						+ "`c!lister {Status};[Type]` : Affiche la liste des écrits avec le status et du type demandés. Status et Type peuvent prendre la valeur « Tout ».\n"
						+ "`c!supprimer {Critère}` : Supprime un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit. __**ATTENTION**__ : Il n'y a pas de confirmation, faites attention à ne pas vous tromper dans le Critère.\n"
						+ "`c!inbox` : Affiche la boîte de réception, contenant les écrits qui n'ont pas pu être ajoutés automatiquement. Attention, l'appel à cette commande supprime le contenu de la boîte.\n"
						+ "__Commandes de modification d'un écrit__ :\n"
						+ "`c!réserver {Critère}` : Réserve un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réserver_pour {Critère};{Nom}` : Réserve un écrit pour quelqu'un d'autre. Le Critère doit être assez fin pour aboutir à un unique écrit."
						+ "`c!libérer {Critère}` : Supprime la réservation d'un écrit si vous êtes la personne l'ayant réservé, ou membre de l'équipe critique, ou que l'écrit a été réservé par procuration. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!status {Critère};{Status}` : Change le status de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!type {Critère};{Type}` : Change le type de l'écrit demandé. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!réservation {Critère}` : Affiche les informations de réservation d'un écrit. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!critiqué {Critère}` : Alias pour c!libérer et c!status En attente. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "`c!valider {Critère}` : Change le type du rapport en Rapport si c'était une idée et fait le même effet que c!critiqué. Le Critère doit être assez fin pour aboutir à un unique écrit.\n"
						+ "\n"
						+ "Version 1.1 — Code source : <https://github.com/cyriellecentori/critibot>"
						, message.getChannel());
			}
		});
		
		commands.put("ajouter", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				try {
					ecrits.add(new Ecrit(args[0],  args[3], Type.getType(args[1]), Status.getStatus(args[2])));
					message.getChannel().sendMessage("Ajouté !").queue();
				} catch (ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!add Nom;Type;Status;URL. "
							+ "Status doit être « Ouvert — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé ». "
							+ "Type doit être « Conte — Rapport — Idée — Autre ».").queue();
				}
			}
			
		});
		
		commands.put("lister", new BotCommand() {

			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				try {
					Status status = (args[0].equalsIgnoreCase("tout") ? null : Status.getStatus(args[0]));
					Type type = null;
					if(args.length > 1)
						type = (args[1].equalsIgnoreCase("tout") ? null : Type.getType(args[1]));
					
					String fullList = "Liste demandée :\n";
					for(Ecrit e : ecrits) {
						if(e.complyWith(type, status))
							fullList += e.toString() + "\n";
					}
					if(fullList.equals("Liste demandée :\\n"))
						fullList = "Aucun écrit ne correspond à votre recherche.";
					sendLongMessage(fullList, message.getChannel());
					
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Utilisation : c!list Status;Type. Il est possible de ne pas spécifier le type si non nécessaire, alors la commande sera c!list Status et tous les types seront affichés.\n"
							+ "Status doit être « Tout — Ouvert — En attente — Abandonné — En pause — Sans nouvelles — Inconnu — Publié — Réservé — Validé — Refusé ». "
							+ "Type doit être « Tout — Conte — Rapport — Idée — Autre ».").queue();
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
					String fullList = "Liste des résultats :\n";
					for(Ecrit e : bot.search(args[0])) {
						fullList += e.toString() + "\n" + "Dernière mise à jour le " + e.getLastUpdate() + "\n—————\n";
					}
					if(fullList.equals("Liste des résultats :\\n")) {
						fullList = "Aucun écrit trouvé.";
					}
					sendLongMessage(fullList, message.getChannel());
				} catch(ArrayIndexOutOfBoundsException e) {
					message.getChannel().sendMessage("Rechercher… Quoi exactement ?").queue();
				}
			}
			
		});
		
		commands.put("s", new BotCommand.Alias(commands.get("rechercher")));
		
		commands.put("réserver", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.reserver(message.getAuthor())) {
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé !").queue();
				} else {
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un status réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au status inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
			}
		});
		
		commands.put("r", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("reserver", new BotCommand.Alias(commands.get("réserver")));
		
		commands.put("libérer", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				if(e.liberer(message.getMember())) {
					message.getChannel().sendMessage("Réservation sur « " + e.getNom() + " » supprimée.").queue();
				} else {
					message.getChannel().sendMessage("Vous n'êtes pas à l'origine de la réservation sur « " + e.getNom() + " » ou vous n'êtes pas de l'équipe critique.").queue();
				}
			}
		});
		
		commands.put("nettoyer", new BotCommand() {
			
			@Override
			public void execute(CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.clean();
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
						message.getChannel().sendMessage("Cet écrit est réservé, libérez le d'abord avec c!libérer pour changer son status.").queue();
					}
				} else
					message.getChannel().sendMessage("Status de « " + e.getNom() + " » changé !").queue();
			}
		});
		
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
				archiver();
				e.promote();
				message.getChannel().sendMessage("Si l'écrit « " + e.getNom() + " » était une idée, c'est maintenant un rapport ! Sinon, rien n'a changé.").queue();
				if(e.getStatus() == Status.RESERVE)
					if(!e.liberer(message.getMember()))
						message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » a déjà été réservé par quelqu'un d'autre. Je vais cependant vous croire sur le fait que vous avez critiqué l'écrit, mais vous avez probablement enfreint une réservation et c'est pas très gentil.").queue();
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
				archiver();
				if(e.reserver(args[1])) {
					message.getChannel().sendMessage("« " + e.getNom() + " » réservé pour " + args[1] + " !").queue();
				} else {
					message.getChannel().sendMessage("L'écrit « " + e.getNom() + " » ne peut pas être réservé. Il l'est peut-être déjà, ou n'a pas un status réservable. Les écrits pouvant être réservés sont les écrits Ouverts, Abandonnés, En Pause, Validés, Sans Nouvelles ou au status inconnu. Vous pouvez également tenter de libérer l'écrit si vous faites partie de l'Équipe Critique ou que l'écrit est réservé depuis plus de trois jours.").queue();
				}
				
			}
		});
		
		commands.put("critiqué", new BotCommand.SearchCommand() {
			
			@Override
			public void process(Ecrit e, CritiBot bot, MessageReceivedEvent message, String[] args) {
				archiver();
				bot.getCommand("libérer").execute(bot, message, new String[] {e.getNom()});
				if(e.getStatus() != Status.RESERVE)
					bot.getCommand("status").execute(bot, message, new String[] {e.getNom(), "En attente"});
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
		
		
	}
	
	public boolean annuler() {
		if(cancel.empty()) {
			return false;
		} else {
			ecrits = cancel.pop();
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
	
	public void archiver() {
		cancel.add(new Vector<Ecrit>());
		for(Ecrit e : ecrits) {
			cancel.peek().add(e.clone());
		}
		if(cancel.size() > 20) {
			cancel.remove(0);
		}
	}

	@Override
	public void onEvent(GenericEvent event) {
		if(!(event instanceof MessageReceivedEvent))
			return;
		
		MessageReceivedEvent mre = (MessageReceivedEvent) event;
		if(mre.getAuthor().getIdLong() == 268478587651358721L) {
			try {
				addNew();
			} catch (IllegalArgumentException | FeedException | IOException e) {
				jda.getPresence().setStatus(OnlineStatus.DO_NOT_DISTURB);
				e.printStackTrace();
			}
		}
		if(!mre.getMessage().getContentRaw().startsWith("c!") || mre.getAuthor().isBot() || mre.getAuthor().getId().equals(jda.getSelfUser().getId()))
			return;
		
		String command = mre.getMessage().getContentRaw().split(" ", 2)[0].split("!", 2)[1];
		String args;
		try {
			args = mre.getMessage().getContentRaw().split(" ", 2)[1];
		} catch(ArrayIndexOutOfBoundsException e) {
			args = "";
		}
		System.out.println(command);
		System.out.println(args);
		System.out.println("——————————");
		MessageChannel chan = mre.getChannel();
		
		try {
			commands.get(command).execute(this, mre, args.split(";"));
		} catch(NullPointerException e) {
			chan.sendMessage("Commande inconnue.").queue();
		}
		
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
