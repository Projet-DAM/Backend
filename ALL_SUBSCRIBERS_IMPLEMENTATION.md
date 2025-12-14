# 📱 BACKEND IMPLÉMENTÉ - Tous les Abonnements

## ✅ Nouveau Endpoint Créé !

### 🎯 Endpoint Principal

```
GET /offers/all-subscribers
```

**Authentification :** JWT Token (Académie ou Admin uniquement)

**Headers :**
```
Authorization: Bearer <votre_token_jwt>
```

---

## 📋 Réponse de l'API

### Structure JSON

```json
{
  "totalSubscribers": 45,
  "offers": [
    {
      "offer": {
        "_id": "65f1a2b3c4d5e6f7g8h9i0j1",
        "name": "Judo Enfants",
        "type": "MONTHLY",
        "price": 70,
        "maxCapacity": 20,
        "subscribersCount": 15
      },
      "subscribers": [
        {
          "subscriptionId": "65f1a2b3c4d5e6f7g8h9i0j2",
          "status": "ACTIVE",
          "startDate": "2025-01-01T00:00:00.000Z",
          "endDate": "2025-01-31T23:59:59.999Z",
          "child": {
            "_id": "65f1a2b3c4d5e6f7g8h9i0j3",
            "nom": "Ben Ali",
            "prenom": "Ahmed",
            "dateNaissance": "2015-05-12T00:00:00.000Z",
            "photoProfil": "https://..."
          },
          "parent": {
            "_id": "65f1a2b3c4d5e6f7g8h9i0j4",
            "nom": "Ben Ali",
            "prenom": "Mohamed",
            "email": "mohamed@example.com",
            "phoneNumber": "+21612345678"
          }
        },
        {
          "subscriptionId": "65f1a2b3c4d5e6f7g8h9i0j5",
          "status": "PENDING",
          "startDate": "2025-01-15T00:00:00.000Z",
          "endDate": "2025-02-14T23:59:59.999Z",
          "child": {
            "_id": "65f1a2b3c4d5e6f7g8h9i0j6",
            "nom": "Trabelsi",
            "prenom": "Fatima",
            "dateNaissance": "2016-03-20T00:00:00.000Z",
            "photoProfil": null
          },
          "parent": {
            "_id": "65f1a2b3c4d5e6f7g8h9i0j7",
            "nom": "Trabelsi",
            "prenom": "Leila",
            "email": "leila@example.com",
            "phoneNumber": "+21698765432"
          }
        }
      ]
    },
    {
      "offer": {
        "_id": "65f1a2b3c4d5e6f7g8h9i0j8",
        "name": "Football Débutants",
        "type": "QUARTERLY",
        "price": 150,
        "maxCapacity": 20,
        "subscribersCount": 20
      },
      "subscribers": [
        // ... 20 inscrits
      ]
    }
  ]
}
```

---

## 🔧 Intégration Android

### 1. Ajouter le Modèle de Données

**Fichier : `response/AllSubscribersResponse.kt`**

```kotlin
data class AllSubscribersResponse(
    val totalSubscribers: Int,
    val offers: List<OfferWithSubscribers>
)

data class OfferWithSubscribers(
    val offer: OfferSummary,
    val subscribers: List<SubscriberDetail>
)

data class OfferSummary(
    val _id: String,
    val name: String,
    val type: String, // MONTHLY, QUARTERLY, ANNUAL
    val price: Double,
    val maxCapacity: Int?,
    val subscribersCount: Int
)

data class SubscriberDetail(
    val subscriptionId: String,
    val status: String, // ACTIVE, PENDING, SUSPENDED, CANCELLED, EXPIRED
    val startDate: String,
    val endDate: String,
    val child: ChildInfo,
    val parent: ParentInfo
)

data class ChildInfo(
    val _id: String,
    val nom: String,
    val prenom: String,
    val dateNaissance: String?,
    val photoProfil: String?
)

data class ParentInfo(
    val _id: String,
    val nom: String,
    val prenom: String,
    val email: String,
    val phoneNumber: String?
)
```

### 2. Ajouter l'Appel API

**Fichier : `network/ApiService.kt`**

```kotlin
interface ApiService {
    // ... autres endpoints

    @GET("offers/all-subscribers")
    suspend fun getAllSubscribers(): Response<AllSubscribersResponse>
}
```

### 3. Créer le ViewModel

**Fichier : `viewmodel/AllSubscribersViewModel.kt`**

```kotlin
class AllSubscribersViewModel(private val apiService: ApiService) : ViewModel() {
    
    private val _subscribersData = MutableLiveData<AllSubscribersResponse>()
    val subscribersData: LiveData<AllSubscribersResponse> = _subscribersData
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    fun loadAllSubscribers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getAllSubscribers()
                if (response.isSuccessful) {
                    _subscribersData.value = response.body()
                } else {
                    _error.value = "Erreur: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Erreur réseau: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### 4. Créer l'Écran (Activity/Fragment)

**Fichier : `AllSubscribersActivity.kt`**

```kotlin
class AllSubscribersActivity : AppCompatActivity() {
    
    private lateinit var viewModel: AllSubscribersViewModel
    private lateinit var adapter: AllSubscribersAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_subscribers)
        
        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewAllSubscribers)
        adapter = AllSubscribersAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        // Setup ViewModel
        viewModel = ViewModelProvider(this).get(AllSubscribersViewModel::class.java)
        
        // Observe data
        viewModel.subscribersData.observe(this) { data ->
            // Update UI with total
            findViewById<TextView>(R.id.tvTotalSubscribers).text = 
                "${data.totalSubscribers} inscrits au total"
            
            // Update RecyclerView
            adapter.submitList(data.offers)
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility = 
                if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Load data
        viewModel.loadAllSubscribers()
    }
}
```

### 5. Créer l'Adapter

**Fichier : `adapter/AllSubscribersAdapter.kt`**

```kotlin
class AllSubscribersAdapter : RecyclerView.Adapter<AllSubscribersAdapter.ViewHolder>() {
    
    private var offers: List<OfferWithSubscribers> = emptyList()
    private val expandedPositions = mutableSetOf<Int>()
    
    fun submitList(newOffers: List<OfferWithSubscribers>) {
        offers = newOffers
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_offer_with_subscribers, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = offers[position]
        holder.bind(item, expandedPositions.contains(position))
        
        holder.itemView.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
            } else {
                expandedPositions.add(position)
            }
            notifyItemChanged(position)
        }
    }
    
    override fun getItemCount() = offers.size
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvOfferName: TextView = view.findViewById(R.id.tvOfferName)
        private val tvOfferType: TextView = view.findViewById(R.id.tvOfferType)
        private val tvOfferPrice: TextView = view.findViewById(R.id.tvOfferPrice)
        private val tvSubscribersCount: TextView = view.findViewById(R.id.tvSubscribersCount)
        private val recyclerViewSubscribers: RecyclerView = view.findViewById(R.id.recyclerViewSubscribers)
        private val ivExpandIcon: ImageView = view.findViewById(R.id.ivExpandIcon)
        
        fun bind(item: OfferWithSubscribers, isExpanded: Boolean) {
            tvOfferName.text = item.offer.name
            tvOfferType.text = item.offer.type
            tvOfferPrice.text = "${item.offer.price} TND"
            
            val capacity = item.offer.maxCapacity?.let { "/$it" } ?: ""
            tvSubscribersCount.text = "👥 ${item.offer.subscribersCount}$capacity"
            
            // Show/hide subscribers list
            recyclerViewSubscribers.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpandIcon.rotation = if (isExpanded) 180f else 0f
            
            if (isExpanded) {
                val subscribersAdapter = SubscribersAdapter(item.subscribers)
                recyclerViewSubscribers.adapter = subscribersAdapter
                recyclerViewSubscribers.layoutManager = LinearLayoutManager(itemView.context)
            }
        }
    }
}
```

---

## 🎨 Layout XML

### `activity_all_subscribers.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/tvTotalSubscribers"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0 inscrits au total"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp"/>

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone"/>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerViewAllSubscribers"
        android:layout_width="match_parent"
        android:layout_height="match_parent"/>

</LinearLayout>
```

---

## 📱 Ajouter au Menu Sidebar

Dans votre `NavigationDrawer` ou `Sidebar`, ajoutez un item visible uniquement pour les académies :

```kotlin
// Dans votre Activity/Fragment qui gère le menu
if (userRole == "ACADEMIE") {
    menu.add("👥 Tous les Abonnements").setOnMenuItemClickListener {
        startActivity(Intent(this, AllSubscribersActivity::class.java))
        true
    }
}
```

---

## ✅ Résumé

### Backend ✅
- ✅ Endpoint créé : `GET /offers/all-subscribers`
- ✅ Sécurisé (Académie/Admin uniquement)
- ✅ Retourne toutes les offres avec leurs inscrits
- ✅ Filtre automatique par académie
- ✅ Calcule le total d'inscrits

### À Faire (Android)
1. Créer les modèles de données
2. Ajouter l'appel API
3. Créer le ViewModel
4. Créer l'écran AllSubscribersActivity
5. Créer l'adapter avec expand/collapse
6. Ajouter au menu sidebar

---

## 🚀 Test de l'API

Vous pouvez tester l'endpoint avec Postman ou curl :

```bash
curl -X GET http://localhost:3000/offers/all-subscribers \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Le backend est prêt ! 🎉
