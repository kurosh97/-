package com.example.artravel.fragments.list

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.artravel.AttractionsRC.OnPlaceItemClickListener
import com.example.artravel.R
import com.example.artravel.model.entity.DBAttraction
import com.example.artravel.model.entity.DBPlace
import com.example.artravel.model.viewmodel.FavouritesViewModel
import kotlinx.android.synthetic.main.fragment_favourites.*
import java.io.ByteArrayOutputStream


class FavouritesFragment : Fragment(), OnPlaceItemClickListener {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favourites, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // FavouritesViewModel
        val ump = ViewModelProviders.of(this).get(FavouritesViewModel::class.java)
        ump.readAllData.observe(this, {
            recycler_view.adapter = FavouritesAdapter(
                requireContext(),
                it.sortedBy { that ->
                    that.name
                }, this
            )
            recycler_view.layoutManager = LinearLayoutManager(requireContext())
        })
    }
<<<<<<< HEAD:app/src/main/java/com/example/artravel/favourites/FavouritesFragment.kt
    override fun onItemClick(item: DBPlace, position: Int) {
        val bundle = Bundle()
=======

    override fun onItemClick(item: Any, position: Int) {

        var item = item as DBPlace

        var bundle = Bundle()

>>>>>>> 6d944558bdb5ed1dce663805791705d2d443c1a1:app/src/main/java/com/example/artravel/fragments/list/FavouritesFragment.kt
        bundle.putString("name", item.name)
        // Compress Bitmap as bytearray and uncompress in Detail Activity
        val stream = ByteArrayOutputStream()
        item.image?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val bytes: ByteArray = stream.toByteArray()
        bundle.putByteArray("bytes", bytes)

        if (item.desc != null) {
            bundle.putString("description", item.desc)
        }
        bundle.putString("lat", item.lat)
        bundle.putString("lon", item.lng)
        findNavController().navigate(
            R.id.action_favouritesFragment_to_attractionsDetailFragment,
            bundle
        )
    }
}