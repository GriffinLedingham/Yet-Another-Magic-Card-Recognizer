import forohfor.scryfall.api.Card;
import forohfor.scryfall.api.MTGCardQuery;

import java.io.IOException;

public class MatchResult
{
    private static String lastCard;
    private static double lastScore;
    private DescContainer result;
    private double score;

    public MatchResult(DescContainer result, double score)
    {
        super();
        this.result = result;
        this.score = score;
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
        return result;
    }

    public Card getCard()
    {
        try
        {
            return MTGCardQuery.getCardByScryfallId(result.getScryfallId());
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getName()
    {
        return result.getName();
    }

    public String set()
    {
        return result.getSet();
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MatchResult other = (MatchResult) obj;
        if (result == null)
        {
            if (other.result != null)
                return false;
        } else if (!result.equals(other.result))
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        if(lastCard != result.getName()) {
            System.out.println("|CARD| " + result.getName() + " : " + score);
            lastScore = score;
            lastCard = result.getName();
        }
        return result.getName() + " : " + score;
    }

    public DescContainer getData()
    {
        return result;
    }
}
