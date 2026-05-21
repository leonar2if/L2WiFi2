package com.example.l2wifi

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class InicioFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CuentasAdapter
    private var editMode = false
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_inicio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        sharedPrefs = requireActivity().getSharedPreferences("L2WiFiPrefs", Context.MODE_PRIVATE)

        cargarCuentas()
        setupFab()
    }

    private fun cargarCuentas() {
        val cuentasJson = sharedPrefs.getString("accounts_json", "[]") ?: "[]"
        val cuentas = JSONArray(cuentasJson)
        adapter = CuentasAdapter(cuentas, editMode, this::onConectarClick, this::onSaldoClick, this::onActivaClick, this::onDeleteClick, this::onDragStart)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        // Configurar drag & drop
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                if (!editMode) return 0
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                return makeMovementFlags(dragFlags, 0)
            }
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from < to) {
                    for (i in from until to) adapter.swap(i, i + 1)
                } else {
                    for (i in from downTo to + 1) adapter.swap(i, i - 1)
                }
                adapter.notifyItemMoved(from, to)
                guardarOrden()
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        touchHelper.attachToRecyclerView(recyclerView)

        // Iniciar drag desde el botón de arrastre
        adapter.setOnDragListener { viewHolder ->
            touchHelper.startDrag(viewHolder)
        }
    }

    private fun guardarOrden() {
        val nuevasCuentas = adapter.getCurrentList()
        val jsonArray = JSONArray()
        for (i in 0 until nuevasCuentas.length()) {
            jsonArray.put(nuevasCuentas.getJSONObject(i))
        }
        sharedPrefs.edit().putString("accounts_json", jsonArray.toString()).apply()
    }

    private fun setupFab() {
        val fab = (requireActivity() as MainActivity).getFab()
        fab.setOnClickListener {
            if (editMode) {
                editMode = false
                (requireActivity() as MainActivity).setFabIcon(R.drawable.ic_add)
                adapter.setEditMode(false)
            } else {
                mostrarDialogoAgregarCuenta()
            }
        }
    }

    private fun mostrarDialogoAgregarCuenta() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_account, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        val etUser = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dialogUsername)
        val etPass = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dialogPassword)
        dialogView.findViewById<android.widget.Button>(R.id.dialogSave).setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val cuentasJson = sharedPrefs.getString("accounts_json", "[]") ?: "[]"
                val cuentas = JSONArray(cuentasJson)
                val nueva = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                    put("uuid", "")
                }
                cuentas.put(nueva)
                sharedPrefs.edit().putString("accounts_json", cuentas.toString()).apply()
                cargarCuentas()
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Completa ambos campos", Toast.LENGTH_SHORT).show()
            }
        }
        dialogView.findViewById<android.widget.Button>(R.id.dialogCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    fun toggleEditMode() {
        editMode = !editMode
        adapter.setEditMode(editMode)
        if (editMode) {
            (requireActivity() as MainActivity).setFabIcon(android.R.drawable.ic_menu_save) // o un texto OK
        } else {
            (requireActivity() as MainActivity).setFabIcon(R.drawable.ic_add)
            guardarOrden()
        }
    }

    private fun onConectarClick(position: Int, cuenta: JSONObject) {
        val user = cuenta.getString("username")
        val pass = cuenta.getString("password")
        // Llamada asíncrona a red
        Thread {
            val uuid = EtecsaNetworkClient.iniciarSesionEtecsa(user, pass)
            requireActivity().runOnUiThread {
                if (uuid != null && uuid != "SIN_SALDO") {
                    // Marcar esta cuenta como activa
                    val cuentas = adapter.getCurrentList()
                    for (i in 0 until cuentas.length()) {
                        cuentas.getJSONObject(i).put("uuid", if (i == position) uuid else "")
                    }
                    sharedPrefs.edit().putString("accounts_json", cuentas.toString()).apply()
                    cargarCuentas()
                    // Abrir ContadorFragment con esta cuenta
                    val fragment = ContadorFragment.newInstance(cuenta.getString("username"), uuid)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                    (requireActivity() as MainActivity).setFabVisibility(false)
                } else {
                    Toast.makeText(requireContext(), if (uuid == "SIN_SALDO") "Saldo insuficiente" else "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun onSaldoClick(position: Int, cuenta: JSONObject) {
        val uuid = cuenta.optString("uuid", "")
        if (uuid.isEmpty()) {
            Toast.makeText(requireContext(), "No hay sesión activa", Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val saldo = EtecsaNetworkClient.consultarSaldoEtecsa(uuid)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Saldo: $saldo", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun onActivaClick(position: Int, cuenta: JSONObject) {
        val uuid = cuenta.optString("uuid", "")
        if (uuid.isNotEmpty()) {
            val fragment = ContadorFragment.newInstance(cuenta.getString("username"), uuid)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
            (requireActivity() as MainActivity).setFabVisibility(false)
        }
    }

    private fun onDeleteClick(position: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirm_delete, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        dialogView.findViewById<android.widget.Button>(R.id.confirmOk).setOnClickListener {
            val cuentas = adapter.getCurrentList()
            cuentas.remove(position)
            sharedPrefs.edit().putString("accounts_json", cuentas.toString()).apply()
            cargarCuentas()
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.confirmCancel).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun onDragStart(viewHolder: RecyclerView.ViewHolder) {
        // El touchHelper inicia el drag
    }
}