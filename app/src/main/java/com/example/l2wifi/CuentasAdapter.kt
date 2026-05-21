package com.example.l2wifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class CuentasAdapter(
    private var cuentas: JSONArray,
    private var editMode: Boolean,
    private val onConectar: (Int, JSONObject) -> Unit,
    private val onSaldo: (Int, JSONObject) -> Unit,
    private val onActiva: (Int, JSONObject) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onDragStart: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<CuentasAdapter.ViewHolder>() {

    private var dragListener: ((RecyclerView.ViewHolder) -> Unit)? = null

    fun setOnDragListener(listener: (RecyclerView.ViewHolder) -> Unit) {
        dragListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cuenta, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cuenta = cuentas.getJSONObject(position)
        val username = cuenta.getString("username")
        val uuid = cuenta.optString("uuid", "")
        val isActive = uuid.isNotEmpty()

        holder.emailText.text = username
        holder.emailText.isEnabled = !editMode

        // Mostrar/ocultar según modo edición y estado activo
        if (editMode) {
            // En modo edición, ocultar botones normales y el botón Activa
            holder.buttonWifi.visibility = View.GONE
            holder.buttonMoney.visibility = View.GONE
            holder.btnActiva.visibility = View.GONE
            // Mostrar delete y drag, pero si la cuenta está activa, deshabilitarlos visualmente
            if (isActive) {
                holder.btnDelete.visibility = View.GONE
                holder.btnDrag.visibility = View.GONE
                holder.emailText.alpha = 0.5f
            } else {
                holder.btnDelete.visibility = View.VISIBLE
                holder.btnDrag.visibility = View.VISIBLE
                holder.emailText.alpha = 1f
            }
        } else {
            // Modo normal
            holder.buttonWifi.visibility = if (!isActive) View.VISIBLE else View.GONE
            holder.buttonMoney.visibility = if (!isActive) View.VISIBLE else View.GONE
            holder.btnActiva.visibility = if (isActive) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = View.GONE
            holder.btnDrag.visibility = View.GONE
            holder.emailText.alpha = 1f
            if (isActive) holder.btnActiva.setTextColor(0xFF4CAF50.toInt())
        }

        // Listeners
        holder.buttonWifi.setOnClickListener { onConectar(position, cuenta) }
        holder.buttonMoney.setOnClickListener { onSaldo(position, cuenta) }
        holder.btnActiva.setOnClickListener { onActiva(position, cuenta) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
        holder.btnDrag.setOnClickListener { dragListener?.invoke(holder) }
    }

    override fun getItemCount() = cuentas.length()

    fun setEditMode(edit: Boolean) {
        editMode = edit
        notifyDataSetChanged()
    }

    fun swap(from: Int, to: Int) {
        val fromObj = cuentas.getJSONObject(from)
        val toObj = cuentas.getJSONObject(to)
        cuentas.put(from, toObj)
        cuentas.put(to, fromObj)
        notifyItemMoved(from, to)
    }

    fun getCurrentList(): JSONArray = cuentas

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val emailText: TextView = itemView.findViewById(R.id.emailText)
        val buttonWifi: ImageButton = itemView.findViewById(R.id.buttonWifi)
        val buttonMoney: ImageButton = itemView.findViewById(R.id.buttonMoney)
        val btnActiva: Button = itemView.findViewById(R.id.btnActiva)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnDrag: ImageButton = itemView.findViewById(R.id.btnDrag)
    }
}