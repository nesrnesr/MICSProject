package uni.lu.mics.mics_project.nmbd.app.service.ExtendedList;

import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.LinkedList;

public class ExtendedListEvent {
    private LinkedList<String> ids;
    private HashMap<String, String> names;
    private HashMap<String, String> dates;
    private HashMap<String, String>  categories;
    private HashMap<String, String> addresses;

    public ExtendedListEvent(){
        this.ids = new LinkedList<>();
        this.names = new HashMap<>();
        this.dates = new HashMap<>();
        this.categories = new HashMap<>();
        this.addresses = new HashMap<>();
    }

    public LinkedList<String> getIdList(){
        return ids;
    }

    public void removeElement(int pos){
        names.remove(ids.get(pos));
        ids.remove(ids.get(pos));
        dates.remove(ids.get(pos));
        categories.remove(ids.get(pos));
        addresses.remove(ids.get(pos));
    }

    public void clearLists(){
        names.clear();
        ids.clear();
        dates.clear();
        categories.clear();
        addresses.clear();
    }


    public String getId(int pos){
    return ids.get(pos);
}
    public String getName (int pos){
        return names.get(ids.get(pos));
    }
    public String getDate (int pos){
        return dates.get(ids.get(pos));
    }
    public String getCategory (int pos){
        return categories.get(ids.get(pos));
    }
    public String getAddress (int pos){
        return addresses.get(ids.get(pos));
    }

    public void addElement(String name, String id, String date, String category, String address){
        ids.add(id);
        names.put(id, name);
        dates.put(id, date);
        categories.put(id, category);
        addresses.put(id, address);
    }

    public int getSize(){
        return ids.size();
    }

    public int getIdIndexOfLast(){
        return ids.size()-1;
    }
}
