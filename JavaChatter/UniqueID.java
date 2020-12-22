import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UniqueID
{
    private static List<Integer> IDList = new ArrayList<Integer>();
    private static final int Range = 100;
    private static int Index = 0;

    static 
    {
        for (int i = 0; i < Range; i++) 
        {
			IDList.add(i);
		}
		Collections.shuffle(IDList);
    }

    private UniqueID() 
    {        
    }
    public static int getID()
    {
        if (Index > IDList.size() - 1) 
        {
            Index = 0;
        }
		return IDList.get(Index++);
    }
}
