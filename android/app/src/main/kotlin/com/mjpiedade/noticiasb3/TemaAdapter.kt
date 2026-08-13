package com.mjpiedade.noticiasb3

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter dos cartões de tema. Cada cartão fica colapsado por defeito; toque
 * na cabeça alterna entre colapsado/expandido (mostra resumo, impacto e a lista
 * de notícias-membro).
 *
 * O cartão tem uma barra vertical colorida à esquerda com a cor do sinal, feita
 * em runtime com um LayerDrawable por cima do drawable base `cartao_fundo`.
 */
class TemaAdapter : RecyclerView.Adapter<TemaAdapter.VH>() {

    private val temas = mutableListOf<Tema>()
    private val expandidos = HashSet<Int>()

    fun submeter(novos: List<Tema>) {
        temas.clear()
        temas.addAll(novos)
        // Mantemos expandidos apenas se ainda existirem
        expandidos.retainAll { it < temas.size }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = temas.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_tema, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = temas[position]
        holder.ligar(t, expandidos.contains(position)) {
            val estavaAberto = expandidos.contains(position)
            if (estavaAberto) expandidos.remove(position) else expandidos.add(position)
            notifyItemChanged(position)
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val raiz: View = v.findViewById(R.id.raiz_tema)
        private val cabeca: View = v.findViewById(R.id.cabeca)
        private val selo: TextView = v.findViewById(R.id.selo)
        private val titulo: TextView = v.findViewById(R.id.titulo)
        private val nNoticias: TextView = v.findViewById(R.id.n_noticias)
        private val chevron: ImageView = v.findViewById(R.id.chevron)
        private val corpo: View = v.findViewById(R.id.corpo)
        private val resumoTitulo: View = v.findViewById(R.id.bloco_resumo_titulo)
        private val resumo: TextView = v.findViewById(R.id.resumo)
        private val impactoTitulo: View = v.findViewById(R.id.bloco_impacto_titulo)
        private val impacto: TextView = v.findViewById(R.id.impacto)
        private val noticiasTitulo: TextView = v.findViewById(R.id.bloco_noticias_titulo)
        private val listaNoticias: LinearLayout = v.findViewById(R.id.lista_noticias)

        fun ligar(t: Tema, aberto: Boolean, aoTocar: () -> Unit) {
            // Cor da borda esquerda: envolve o cartao_fundo num LayerDrawable com uma
            // faixa colorida à esquerda (4dp). Recria a cada bind por causa do recycling.
            val ctx = raiz.context
            val base = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.cartao_fundo)!!.mutate()
            val faixa = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(t.sinal.cor)
            }
            val camadas = LayerDrawable(arrayOf(base, faixa))
            // Faixa colorida na aresta esquerda, 4dp de largura, altura toda do cartão.
            // setLayerGravity+setLayerSize são API 23+ (temos minSdk 26).
            val faixaLarguraPx = (4 * ctx.resources.displayMetrics.density).toInt()
            camadas.setLayerGravity(1, android.view.Gravity.START)
            camadas.setLayerSize(1, faixaLarguraPx, -1)
            raiz.background = camadas

            // Selo
            selo.text = t.sinal.rotulo
            selo.backgroundTintList = ColorStateList.valueOf(t.sinal.cor)

            titulo.text = t.titulo
            nNoticias.text = ctx.resources.getQuantityString(R.plurals.contagem_noticias, t.nNoticias, t.nNoticias)

            chevron.rotation = if (aberto) 180f else 0f

            if (aberto) {
                corpo.visibility = View.VISIBLE
                if (t.resumo.isNotBlank()) {
                    resumoTitulo.visibility = View.VISIBLE
                    resumo.visibility = View.VISIBLE
                    resumo.text = t.resumo
                } else {
                    resumoTitulo.visibility = View.GONE
                    resumo.visibility = View.GONE
                }
                if (t.impacto.isNotBlank()) {
                    impactoTitulo.visibility = View.VISIBLE
                    impacto.visibility = View.VISIBLE
                    impacto.text = t.impacto
                } else {
                    impactoTitulo.visibility = View.GONE
                    impacto.visibility = View.GONE
                }
                if (t.noticias.isNotEmpty()) {
                    noticiasTitulo.visibility = View.VISIBLE
                    listaNoticias.visibility = View.VISIBLE
                    noticiasTitulo.text = ctx.getString(R.string.noticias) + " (${t.noticias.size})"
                    popularNoticias(t.noticias)
                } else {
                    noticiasTitulo.visibility = View.GONE
                    listaNoticias.visibility = View.GONE
                }
            } else {
                corpo.visibility = View.GONE
            }

            cabeca.setOnClickListener { aoTocar() }
        }

        private fun popularNoticias(noticias: List<NoticiaItem>) {
            listaNoticias.removeAllViews()
            val inflater = LayoutInflater.from(listaNoticias.context)
            for (n in noticias) {
                val v = inflater.inflate(R.layout.item_noticia, listaNoticias, false)
                v.findViewById<TextView>(R.id.n_titulo).text = n.titulo
                v.findViewById<TextView>(R.id.n_fonte).text = n.fonte
                v.findViewById<TextView>(R.id.n_hora).text = when {
                    n.data.isBlank() && n.hora.isBlank() -> ""
                    n.hora.isBlank() -> n.data
                    n.data.isBlank() -> n.hora
                    else -> "${n.data} · ${n.hora}"
                }
                listaNoticias.addView(v)
            }
        }
    }
}
